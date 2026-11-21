from __future__ import annotations

import argparse
import time
import tracemalloc
from collections import defaultdict
from datetime import timedelta

import pandas as pd
import psutil

from data.topic_repository import get_topics
from simulator.behavior import BehaviorModel
from simulator.config import TRAINING_CONFIG, DEV_CONFIG, SimulationConfig
from simulator.events import SimulatedEvent, SimulatedEventType
from simulator.posts import SimulatedPost
from simulator.sessions import SimulatedSession
from simulator.simulation import SimulationEngine
from training.analysis import DatasetAnalyzer, print_simulation_summary
from training.dataset import DatasetBuilder, ParquetBatchWriter


def run_simulation(
        config: SimulationConfig,
        *,
        keep_events: bool | None = None,
        run_analyzer: bool = True,
) -> dict:
    """
    Run a full simulation for the given config.

    Events are streamed into Parquet dataset rows in batches.
    For small/dev runs, raw events are retained for full analyzer checks.
    """

    if keep_events is None:
        keep_events = config.user_count <= 50 and config.session_count <= 200

    process = psutil.Process()
    tracemalloc.start()
    started = time.perf_counter()
    mem_before = process.memory_info().rss

    topics = get_topics()
    print(f"Loaded {len(topics)} topics.")

    behavior_model = BehaviorModel()
    engine = SimulationEngine(
        topics=topics,
        behavior_model=behavior_model,
        config=config,
    )

    users = engine.create_users()
    print(f"Created {len(users)} simulated users.")

    posts = engine.create_posts(
        author_ids=[user.user_id for user in users],
    )
    print(f"Created {len(posts)} simulated posts.")

    sessions = engine.create_sessions(users=users)
    print(f"Created {len(sessions)} simulated sessions.")

    users_by_id = {user.user_id: user for user in users}

    sessions = sorted(
        sessions,
        key=lambda session: (
            session.started_at,
            session.session_id,
        ),
    )

    for i in range(min(10, len(sessions) - 1)):
        current = sessions[i]
        next_session = sessions[i + 1]

        current_end = (
                current.started_at
                + timedelta(seconds=current.duration_seconds)
        )

        if current_end > next_session.started_at:
            print(
                "OVERLAP:",
                current.session_id,
                "->",
                next_session.session_id,
            )

    builder = DatasetBuilder(
        users=users,
        posts=posts,
        sessions=sessions,
    )

    output_path = config.parquet_path
    if output_path.exists():
        output_path.unlink()

    writer = ParquetBatchWriter(
        path=output_path,
        batch_size=config.batch_size,
    )

    all_events = [] if keep_events else None
    event_count = 0
    row_buffer: list[dict] = []
    streamed_event_checks = _empty_event_check_counts()
    posts_by_id = {post.post_id: post for post in posts}

    for index, session in enumerate(sessions, start=1):
        user = users_by_id[session.user_id]

        events = engine.simulate_session(
            user=user,
            session=session,
            posts=posts,
        )
        event_count += len(events)

        # Always validate event invariants during generation (no post-hoc fixes).
        _accumulate_event_checks(
            streamed_event_checks,
            events,
            sessions_by_id={session.session_id: session},
            posts_by_id=posts_by_id,
        )

        if keep_events:
            all_events.extend(events)

        rows = builder.build_rows(events)
        row_buffer.extend(rows)

        if len(row_buffer) >= config.batch_size:
            writer.add_rows(row_buffer)
            row_buffer = []

        if index % 1_000 == 0 or index == len(sessions):
            print(
                f"  sessions={index:,}/{len(sessions):,} "
                f"events={event_count:,} rows_flushed={writer.rows_written:,}"
            )

    if row_buffer:
        writer.add_rows(row_buffer)

    dataset_rows = writer.close()

    elapsed = time.perf_counter() - started
    current, peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()
    mem_after = process.memory_info().rss

    dataset = pd.read_parquet(output_path)

    title = (
        "LARGE SIMULATION"
        if config.session_count >= 1_000
        else "DEV SIMULATION"
    )
    print_simulation_summary(
        title=title,
        user_count=len(users),
        post_count=len(posts),
        session_count=len(sessions),
        event_count=event_count,
        dataset=dataset,
    )

    analyzer_ok = True
    if run_analyzer:
        print()
        print("========== DATASET ANALYZER ==========")
        analyzer = DatasetAnalyzer(
            dataset,
            events=all_events,
            sessions=sessions,
            posts=posts,
        )
        analyzer.analyze()

        if not keep_events:
            print()
            print("========== STREAMED EVENT INTEGRITY ==========")
            for name, count in streamed_event_checks.items():
                status = "PASS" if count == 0 else "WARNING"
                print(f"[{status}] {name}: {count}")

        analyzer_ok = _analyzer_passed(
            dataset,
            all_events,
            sessions,
            posts,
            streamed_event_checks=streamed_event_checks,
        )

    file_size = output_path.stat().st_size if output_path.exists() else 0

    print()
    print("========== RUN METRICS ==========")
    print(f"execution time:     {elapsed:.2f}s")
    print(f"rss before:         {mem_before / (1024 ** 2):.1f} MiB")
    print(f"rss after:          {mem_after / (1024 ** 2):.1f} MiB")
    print(f"tracemalloc peak:   {peak / (1024 ** 2):.1f} MiB")
    print(f"output path:        {output_path.resolve()}")
    print(f"output file size:   {file_size / (1024 ** 2):.2f} MiB")
    print(f"dataset rows:       {dataset_rows:,}")
    print(f"analyzer passed:    {analyzer_ok}")

    return {
        "users": len(users),
        "posts": len(posts),
        "sessions": len(sessions),
        "events": event_count,
        "dataset_rows": dataset_rows,
        "elapsed_seconds": elapsed,
        "output_path": str(output_path.resolve()),
        "output_bytes": file_size,
        "rss_before": mem_before,
        "rss_after": mem_after,
        "tracemalloc_peak": peak,
        "analyzer_passed": analyzer_ok,
        "dataset": dataset,
    }


def _empty_event_check_counts() -> dict[str, int]:
    return {
        "Event: VIEW_START without IMPRESSION": 0,
        "Event: VIEW_END without VIEW_START": 0,
        "Event: engagement without VIEW_END": 0,
        "Event: invalid MEDIA_COMPLETE": 0,
        "Event: duplicate engagement actions": 0,
        "Event: non-monotonic timestamps": 0,
        "Event: outside session boundaries": 0,
        "Event: before post creation": 0,
    }


def _accumulate_event_checks(
        counts: dict[str, int],
        events: list[SimulatedEvent],
        sessions_by_id: dict[str, SimulatedSession],
        posts_by_id: dict[int, SimulatedPost],
) -> None:
    """Same invariants as DatasetAnalyzer._event_stream_checks, streamed."""

    if not events:
        return

    grouped: dict[tuple[int, int, str | None], list[SimulatedEvent]] = defaultdict(list)
    for event in events:
        grouped[(event.user_id, event.post_id, event.session_id)].append(event)

    engagement_types = {
        SimulatedEventType.LIKE,
        SimulatedEventType.SAVE,
        SimulatedEventType.COMMENT,
        SimulatedEventType.SHARE,
    }

    for (user_id, post_id, session_id), evs in grouped.items():
        types = [event.event_type for event in evs]
        type_set = set(types)

        if (
            SimulatedEventType.VIEW_START in type_set
            and SimulatedEventType.IMPRESSION not in type_set
        ):
            counts["Event: VIEW_START without IMPRESSION"] += 1

        if (
            SimulatedEventType.VIEW_END in type_set
            and SimulatedEventType.VIEW_START not in type_set
        ):
            counts["Event: VIEW_END without VIEW_START"] += 1

        if (
            type_set & engagement_types
            and SimulatedEventType.VIEW_END not in type_set
        ):
            counts["Event: engagement without VIEW_END"] += 1

        if SimulatedEventType.MEDIA_COMPLETE in type_set:
            post = posts_by_id.get(post_id)
            if (
                post is None
                or post.content_type != "VIDEO"
                or SimulatedEventType.VIEW_START not in type_set
                or SimulatedEventType.VIEW_END not in type_set
            ):
                counts["Event: invalid MEDIA_COMPLETE"] += 1

        for engagement in engagement_types:
            if types.count(engagement) > 1:
                counts["Event: duplicate engagement actions"] += 1

        for earlier, later in zip(evs, evs[1:]):
            if later.occurred_at < earlier.occurred_at:
                counts["Event: non-monotonic timestamps"] += 1
                break

        session = sessions_by_id.get(session_id) if session_id else None
        if session is not None:
            session_end = (
                session.started_at
                + timedelta(seconds=session.duration_seconds)
            )
            if any(
                event.occurred_at < session.started_at
                or event.occurred_at > session_end
                for event in evs
            ):
                counts["Event: outside session boundaries"] += 1

        post = posts_by_id.get(post_id)
        if post is not None and any(
            event.occurred_at < post.created_at
            for event in evs
        ):
            counts["Event: before post creation"] += 1


def _analyzer_passed(
        dataset: pd.DataFrame,
        events,
        sessions,
        posts,
        streamed_event_checks: dict[str, int] | None = None,
) -> bool:
    """Return True only if dataset + event integrity counts are all zero."""

    df = dataset
    checks = [
        int(((df["content_type"] != "VIDEO") & df["completion_ratio"].notna()).sum()),
        int((df["dwell_ms"] < 0).sum()),
        int((df["completion_ratio"] > 1).sum()),
        int((df["completion_ratio"] < 0).sum()),
        int(((df["viewed"] == 1) & (df["impressed"] == 0)).sum()),
        int(((df["liked"] == 1) & (df["viewed"] == 0)).sum()),
        int(((df["saved"] == 1) & (df["viewed"] == 0)).sum()),
        int(((df["commented"] == 1) & (df["viewed"] == 0)).sum()),
        int(((df["shared"] == 1) & (df["viewed"] == 0)).sum()),
        int(((df["completed"] == 1) & (df["skipped"] == 1)).sum()),
        int(df.duplicated(subset=["user_id", "post_id", "session_id"]).sum()),
    ]

    event_failures = 0
    if events:
        counts = _empty_event_check_counts()
        _accumulate_event_checks(
            counts,
            events,
            sessions_by_id={s.session_id: s for s in sessions},
            posts_by_id={p.post_id: p for p in posts},
        )
        event_failures = sum(counts.values())
    elif streamed_event_checks is not None:
        event_failures = sum(streamed_event_checks.values())

    return event_failures == 0 and all(c == 0 for c in checks)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Postify recommendation simulator",
    )
    parser.add_argument(
        "mode",
        nargs="?",
        default="dev",
        choices=["dev", "training", "both"],
        help="Dataset scale to generate (default: dev)",
    )
    args = parser.parse_args()

    if args.mode in ("dev", "both"):
        print("=" * 60)
        print("Running DEV configuration")
        print("=" * 60)
        result = run_simulation(TRAINING_CONFIG, keep_events=False)
        if not result["analyzer_passed"]:
            raise SystemExit("Dev simulation failed analyzer integrity checks.")

    if args.mode in ("training", "both"):
        print()
        print("=" * 60)
        print("Running TRAINING configuration")
        print("=" * 60)
        result = run_simulation(TRAINING_CONFIG, keep_events=False)
        if not result["analyzer_passed"]:
            raise SystemExit(
                "Training simulation failed analyzer integrity checks."
            )


if __name__ == "__main__":
    main()
