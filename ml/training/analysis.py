from __future__ import annotations

from collections import defaultdict
from datetime import timedelta

import pandas as pd

from simulator.events import SimulatedEvent, SimulatedEventType
from simulator.posts import SimulatedPost
from simulator.sessions import SimulatedSession
from training.dataset import (
    FUNNEL_COLUMNS,
    INPUT_FEATURE_COLUMNS,
    TARGET_COLUMNS,
)


class DatasetAnalyzer:

    def __init__(
            self,
            dataset: pd.DataFrame,
            events: list[SimulatedEvent] | None = None,
            sessions: list[SimulatedSession] | None = None,
            posts: list[SimulatedPost] | None = None
    ):
        self.dataset = dataset
        self.events = events or []
        self.sessions = {
            session.session_id: session
            for session in (sessions or [])
        }
        self.posts = {
            post.post_id: post
            for post in (posts or [])
        }

    # ============================================================
    # MAIN ANALYSIS
    # ============================================================

    def analyze(self) -> None:

        if self.dataset.empty:
            print("Dataset is empty.")
            return

        print()
        print("========================================")
        print("        DATASET QUALITY ANALYSIS")
        print("========================================")

        self._feature_target_contract()
        self._dataset_size()
        self._missing_values()
        self._interaction_rates()
        self._watch_behavior()
        self._engagement_rates()
        self._relevance_analysis()
        self._content_analysis()
        self._user_analysis()
        self._data_integrity()

    # ============================================================
    # LEAKAGE CONTRACT
    # ============================================================

    def _feature_target_contract(self) -> None:

        print()
        print("========== FEATURE / TARGET CONTRACT ==========")

        print("INPUT FEATURES (pre-interaction):")
        for column in INPUT_FEATURE_COLUMNS:
            present = column in self.dataset.columns
            print(f"  [{'OK' if present else 'MISSING'}] {column}")

        print()
        print("FUNNEL COLUMNS (same-exposure context, not LIKE inputs):")
        for column in FUNNEL_COLUMNS:
            present = column in self.dataset.columns
            print(f"  [{'OK' if present else 'MISSING'}] {column}")

        print()
        print("TARGETS (never use as inputs for same-row engagement):")
        for column in TARGET_COLUMNS:
            present = column in self.dataset.columns
            print(f"  [{'OK' if present else 'MISSING'}] {column}")

    # ============================================================
    # DATASET SIZE
    # ============================================================

    def _dataset_size(self) -> None:

        print()
        print("========== DATASET SIZE ==========")

        print(f"Rows:     {len(self.dataset):,}")
        print(f"Columns:  {len(self.dataset.columns)}")
        print(f"Users:    {self.dataset['user_id'].nunique():,}")
        print(f"Posts:    {self.dataset['post_id'].nunique():,}")
        print(f"Sessions: {self.dataset['session_id'].nunique():,}")

    # ============================================================
    # MISSING VALUES
    # ============================================================

    def _missing_values(self) -> None:

        print()
        print("========== MISSING VALUES ==========")

        missing = self.dataset.isnull().sum()
        missing = missing[missing > 0].sort_values(ascending=False)

        if missing.empty:
            print("No missing values.")
            return

        for column, count in missing.items():
            percentage = (count / len(self.dataset)) * 100
            print(f"{column}: {count:,} ({percentage:.2f}%)")

    # ============================================================
    # IMPRESSION / VIEW RATES
    # ============================================================

    def _interaction_rates(self) -> None:

        print()
        print("========== INTERACTION RATES ==========")

        total = len(self.dataset)
        impressed = self.dataset["impressed"].sum()
        viewed = self.dataset["viewed"].sum()

        print(
            f"Impression rate: "
            f"{self._percentage(impressed, total):.2f}%"
        )
        print(
            f"View rate: "
            f"{self._percentage(viewed, total):.2f}%"
        )

        if impressed > 0:
            print(
                f"View / impression: "
                f"{self._percentage(viewed, impressed):.2f}%"
            )

    # ============================================================
    # WATCH / MEDIA BEHAVIOR
    # ============================================================

    def _watch_behavior(self) -> None:

        print()
        print("========== WATCH BEHAVIOR ==========")

        viewed = self.dataset[self.dataset["viewed"] == 1]

        if viewed.empty:
            print("No viewed posts.")
            return

        print(
            f"Average dwell time: "
            f"{viewed['dwell_ms'].mean():,.0f} ms"
        )

        completion = viewed[viewed["completion_ratio"].notna()]

        if completion.empty:
            print("Average completion: N/A")
            print("Median completion: N/A")
            print("Completion rate: N/A")
        else:
            print(
                f"Average completion: "
                f"{completion['completion_ratio'].mean():.3f}"
            )
            print(
                f"Median completion: "
                f"{completion['completion_ratio'].median():.3f}"
            )
            completed = completion["completed"].sum()
            print(
                f"Completion rate: "
                f"{self._percentage(completed, len(completion)):.2f}%"
            )

        skipped = viewed["skipped"].sum()
        print(
            f"Skip rate: "
            f"{self._percentage(skipped, len(viewed)):.2f}%"
        )

    # ============================================================
    # ENGAGEMENT
    # ============================================================

    def _engagement_rates(self) -> None:

        print()
        print("========== ENGAGEMENT ==========")

        total = len(self.dataset)
        viewed = max(int(self.dataset["viewed"].sum()), 1)

        metrics = {
            "Like rate (all rows)": "liked",
            "Save rate (all rows)": "saved",
            "Comment rate (all rows)": "commented",
            "Share rate (all rows)": "shared",
        }

        for name, column in metrics.items():
            count = self.dataset[column].sum()
            print(
                f"{name}: "
                f"{self._percentage(count, total):.2f}% "
                f"({int(count)} events)"
            )

        print()
        print("Engagement among viewed rows:")
        for label, column in [
            ("Like", "liked"),
            ("Save", "saved"),
            ("Comment", "commented"),
            ("Share", "shared"),
        ]:
            count = self.dataset.loc[
                self.dataset["viewed"] == 1, column
            ].sum()
            print(
                f"  {label} / viewed: "
                f"{self._percentage(count, viewed):.2f}%"
            )

    # ============================================================
    # RELEVANCE
    # ============================================================

    def _relevance_analysis(self) -> None:

        print()
        print("========== TOPIC RELEVANCE ==========")

        print(
            f"Average relevance: "
            f"{self.dataset['topic_relevance'].mean():.3f}"
        )
        print(
            f"Median relevance: "
            f"{self.dataset['topic_relevance'].median():.3f}"
        )
        print(
            f"Minimum relevance: "
            f"{self.dataset['topic_relevance'].min():.3f}"
        )
        print(
            f"Maximum relevance: "
            f"{self.dataset['topic_relevance'].max():.3f}"
        )

        print()
        self._compare_relevance("liked", "liked")
        self._compare_relevance("saved", "saved")
        self._compare_relevance("commented", "commented")
        self._compare_relevance("shared", "shared")

        video = self.dataset[
            (self.dataset["content_type"] == "VIDEO")
            & (self.dataset["viewed"] == 1)
        ]

        if not video.empty and video["completed"].sum() > 0:
            completed = video[video["completed"] == 1]
            abandoned = video[video["skipped"] == 1]

            print()
            print(
                f"Relevance of completed videos: "
                f"{completed['topic_relevance'].mean():.3f}"
            )

            if not abandoned.empty:
                print(
                    f"Relevance of skipped videos: "
                    f"{abandoned['topic_relevance'].mean():.3f}"
                )

    def _compare_relevance(self, column: str, label: str) -> None:

        if column not in self.dataset.columns:
            return

        positive = self.dataset[self.dataset[column] == 1]
        negative = self.dataset[self.dataset[column] == 0]

        if positive.empty:
            print(f"Relevance of {label} posts: N/A (none)")
            return

        print(
            f"Relevance of {label} posts: "
            f"{positive['topic_relevance'].mean():.3f}"
        )

        if not negative.empty:
            print(
                f"Relevance of non-{label} posts: "
                f"{negative['topic_relevance'].mean():.3f}"
            )

    # ============================================================
    # CONTENT TYPE
    # ============================================================

    def _content_analysis(self) -> None:

        print()
        print("========== CONTENT TYPES ==========")

        content_counts = self.dataset["content_type"].value_counts()

        for content_type, count in content_counts.items():
            percentage = (count / len(self.dataset)) * 100
            print(f"{content_type}: {count:,} ({percentage:.2f}%)")

        print()
        print("Engagement by content type:")

        grouped = (
            self.dataset
            .groupby("content_type")
            .agg(
                rows=("post_id", "count"),
                view_rate=("viewed", "mean"),
                like_rate=("liked", "mean"),
                save_rate=("saved", "mean"),
                comment_rate=("commented", "mean"),
                share_rate=("shared", "mean"),
                completion=("completion_ratio", "mean")
            )
        )

        print(grouped.round(3))

    # ============================================================
    # USER ANALYSIS
    # ============================================================

    def _user_analysis(self) -> None:

        print()
        print("========== USER BEHAVIOR ==========")

        grouped = (
            self.dataset
            .groupby("user_id")
            .agg(
                interactions=("post_id", "count"),
                views=("viewed", "sum"),
                likes=("liked", "sum"),
                saves=("saved", "sum"),
                avg_relevance=("topic_relevance", "mean"),
                avg_completion=("completion_ratio", "mean"),
                engagement_bias=("user_engagement_bias", "mean"),
            )
        )

        grouped["view_rate"] = (
            grouped["views"] / grouped["interactions"]
        )
        grouped["like_rate"] = (
            grouped["likes"] / grouped["interactions"]
        )

        print(grouped.round(3))

    # ============================================================
    # DATA INTEGRITY
    # ============================================================

    def _data_integrity(self) -> None:

        print()
        print("========== DATA INTEGRITY ==========")

        df = self.dataset
        checks: list[tuple[str, int]] = []

        checks.append((
            "Invalid completion for non-video posts",
            int(
                (
                    (df["content_type"] != "VIDEO")
                    & df["completion_ratio"].notna()
                ).sum()
            )
        ))

        checks.append((
            "Negative dwell times",
            int((df["dwell_ms"] < 0).sum())
        ))

        checks.append((
            "Invalid completion > 1",
            int((df["completion_ratio"] > 1).sum())
        ))

        checks.append((
            "Invalid completion < 0",
            int((df["completion_ratio"] < 0).sum())
        ))

        checks.append((
            "Invalid relevance > 1",
            int((df["topic_relevance"] > 1).sum())
        ))

        checks.append((
            "Invalid relevance < 0",
            int((df["topic_relevance"] < 0).sum())
        ))

        checks.append((
            "Invalid VIDEO media/duration",
            int(
                (
                    (df["content_type"] == "VIDEO")
                    & (
                        (df["media_type"] != "VIDEO")
                        | df["duration_ms"].isna()
                        | (df["duration_ms"] <= 0)
                    )
                ).sum()
            )
        ))

        checks.append((
            "Invalid IMAGE media/duration",
            int(
                (
                    (df["content_type"] == "IMAGE")
                    & (
                        (df["media_type"] != "IMAGE")
                        | df["duration_ms"].notna()
                    )
                ).sum()
            )
        ))

        checks.append((
            "Invalid TEXT media/duration",
            int(
                (
                    (df["content_type"] == "TEXT")
                    & (
                        df["media_type"].notna()
                        | df["duration_ms"].notna()
                    )
                ).sum()
            )
        ))

        checks.append((
            "Viewed without impression",
            int(((df["viewed"] == 1) & (df["impressed"] == 0)).sum())
        ))

        checks.append((
            "Liked without view",
            int(((df["liked"] == 1) & (df["viewed"] == 0)).sum())
        ))

        checks.append((
            "Saved without view",
            int(((df["saved"] == 1) & (df["viewed"] == 0)).sum())
        ))

        checks.append((
            "Commented without view",
            int(((df["commented"] == 1) & (df["viewed"] == 0)).sum())
        ))

        checks.append((
            "Shared without view",
            int(((df["shared"] == 1) & (df["viewed"] == 0)).sum())
        ))

        checks.append((
            "Completed without valid video view",
            int(
                (
                    (df["completed"] == 1)
                    & (
                        (df["content_type"] != "VIDEO")
                        | (df["viewed"] == 0)
                        | df["completion_ratio"].isna()
                    )
                ).sum()
            )
        ))

        checks.append((
            "Skipped and completed simultaneously",
            int(((df["completed"] == 1) & (df["skipped"] == 1)).sum())
        ))

        checks.append((
            "Non-video completed/skipped flags",
            int(
                (
                    (df["content_type"] != "VIDEO")
                    & ((df["completed"] == 1) | (df["skipped"] == 1))
                ).sum()
            )
        ))

        viewed_videos = (
            (df["content_type"] == "VIDEO")
            & (df["viewed"] == 1)
        )

        checks.append((
            "Viewed video missing completed/skipped",
            int(
                (
                    viewed_videos
                    & (df["completed"] == 0)
                    & (df["skipped"] == 0)
                ).sum()
            )
        ))

        checks.append((
            "Video completion inconsistent with dwell/duration",
            int(
                (
                    viewed_videos
                    & df["completion_ratio"].notna()
                    & df["duration_ms"].notna()
                    & (df["dwell_ms"] > 0)
                    & (
                        (
                            df["dwell_ms"] / df["duration_ms"]
                            - df["completion_ratio"]
                        ).abs()
                        > 0.02
                    )
                ).sum()
            )
        ))

        if (
            "impressed_at" in df.columns
            and "post_created_at" in df.columns
        ):
            checks.append((
                "Events before post creation",
                int(
                    (
                        df["impressed_at"].notna()
                        & df["post_created_at"].notna()
                        & (df["impressed_at"] < df["post_created_at"])
                    ).sum()
                )
            ))

        if (
            "impressed_at" in df.columns
            and "session_started_at" in df.columns
            and "session_duration_seconds" in df.columns
        ):
            session_end = (
                df["session_started_at"]
                + pd.to_timedelta(
                    df["session_duration_seconds"],
                    unit="s"
                )
            )

            checks.append((
                "Events outside session boundaries",
                int(
                    (
                        df["impressed_at"].notna()
                        & df["session_started_at"].notna()
                        & (
                            (df["impressed_at"] < df["session_started_at"])
                            | (df["impressed_at"] > session_end)
                        )
                    ).sum()
                )
            ))

        # Duplicate impossible engagements within a row (binary flags).
        # Dataset rows are one user/post/session — flags are already 0/1.
        checks.append((
            "Duplicate row keys",
            int(
                df.duplicated(
                    subset=["user_id", "post_id", "session_id"]
                ).sum()
            )
        ))

        # Event-stream checks when raw events are provided.
        if self.events:
            checks.extend(self._event_stream_checks())

        for name, count in checks:
            status = "PASS" if count == 0 else "WARNING"
            print(f"[{status}] {name}: {count}")

    def _event_stream_checks(self) -> list[tuple[str, int]]:

        grouped: dict[
            tuple[int, int, str | None],
            list[SimulatedEvent]
        ] = defaultdict(list)

        for event in self.events:
            grouped[
                (event.user_id, event.post_id, event.session_id)
            ].append(event)

        viewed_without_impression = 0
        view_end_without_start = 0
        engagement_without_view = 0
        media_complete_invalid = 0
        duplicate_engagements = 0
        non_monotonic = 0
        outside_session = 0
        before_post_created = 0

        engagement_types = {
            SimulatedEventType.LIKE,
            SimulatedEventType.SAVE,
            SimulatedEventType.COMMENT,
            SimulatedEventType.SHARE,
        }

        for (user_id, post_id, session_id), events in grouped.items():

            types = [event.event_type for event in events]
            type_set = set(types)

            if (
                SimulatedEventType.VIEW_START in type_set
                and SimulatedEventType.IMPRESSION not in type_set
            ):
                viewed_without_impression += 1

            if (
                SimulatedEventType.VIEW_END in type_set
                and SimulatedEventType.VIEW_START not in type_set
            ):
                view_end_without_start += 1

            if (
                type_set & engagement_types
                and SimulatedEventType.VIEW_END not in type_set
            ):
                engagement_without_view += 1

            if SimulatedEventType.MEDIA_COMPLETE in type_set:
                post = self.posts.get(post_id)
                if (
                    post is None
                    or post.content_type != "VIDEO"
                    or SimulatedEventType.VIEW_START not in type_set
                    or SimulatedEventType.VIEW_END not in type_set
                ):
                    media_complete_invalid += 1

            for engagement in engagement_types:
                if types.count(engagement) > 1:
                    duplicate_engagements += 1

            for earlier, later in zip(events, events[1:]):
                if later.occurred_at < earlier.occurred_at:
                    non_monotonic += 1
                    break

            session = self.sessions.get(session_id) if session_id else None
            if session is not None:
                session_end = (
                    session.started_at
                    + timedelta(seconds=session.duration_seconds)
                )
                if any(
                    event.occurred_at < session.started_at
                    or event.occurred_at > session_end
                    for event in events
                ):
                    outside_session += 1

            post = self.posts.get(post_id)
            if post is not None and any(
                event.occurred_at < post.created_at
                for event in events
            ):
                before_post_created += 1

        return [
            ("Event: VIEW_START without IMPRESSION", viewed_without_impression),
            ("Event: VIEW_END without VIEW_START", view_end_without_start),
            ("Event: engagement without VIEW_END", engagement_without_view),
            ("Event: invalid MEDIA_COMPLETE", media_complete_invalid),
            ("Event: duplicate engagement actions", duplicate_engagements),
            ("Event: non-monotonic timestamps", non_monotonic),
            ("Event: outside session boundaries", outside_session),
            ("Event: before post creation", before_post_created),
        ]

    # ============================================================
    # HELPERS
    # ============================================================

    @staticmethod
    def _percentage(
        value: int | float,
        total: int | float
    ) -> float:

        if total == 0:
            return 0.0

        return (value / total) * 100


def print_simulation_summary(
        *,
        title: str,
        user_count: int,
        post_count: int,
        session_count: int,
        event_count: int,
        dataset: pd.DataFrame,
) -> None:
    """Print scale + rate diagnostics used before ML training."""

    print()
    print(f"========== {title} ==========")

    rows = len(dataset)
    print(f"Users:             {user_count:,}")
    print(f"Posts:             {post_count:,}")
    print(f"Sessions:          {session_count:,}")
    print(f"Events:            {event_count:,}")
    print(f"Dataset rows:      {rows:,}")

    if session_count > 0:
        print(f"Events per session:{event_count / session_count:,.2f}")
    if user_count > 0:
        print(f"Events per user:   {event_count / user_count:,.2f}")
    if post_count > 0:
        print(f"Events per post:   {event_count / post_count:,.2f}")

    if dataset.empty:
        return

    total = len(dataset)
    viewed = int(dataset["viewed"].sum())
    video_viewed = dataset[
        (dataset["content_type"] == "VIDEO") & (dataset["viewed"] == 1)
    ]

    def rate(column: str) -> float:
        return DatasetAnalyzer._percentage(dataset[column].sum(), total)

    print()
    print(f"impression rate:         {rate('impressed'):.2f}%")
    print(f"view rate:               {rate('viewed'):.2f}%")
    print(f"like rate:               {rate('liked'):.2f}%")
    print(f"save rate:               {rate('saved'):.2f}%")
    print(f"comment rate:            {rate('commented'):.2f}%")
    print(f"share rate:              {rate('shared'):.2f}%")

    if not video_viewed.empty:
        completion_rate = DatasetAnalyzer._percentage(
            video_viewed["completed"].sum(),
            len(video_viewed),
        )
        skip_rate = DatasetAnalyzer._percentage(
            video_viewed["skipped"].sum(),
            len(video_viewed),
        )
        print(f"video completion rate:   {completion_rate:.2f}%")
        print(f"skip rate (video views): {skip_rate:.2f}%")
    else:
        print("video completion rate:   N/A")
        print("skip rate (video views): N/A")

    print(f"average relevance:       {dataset['topic_relevance'].mean():.3f}")

    engaged_mask = (
        (dataset["liked"] == 1)
        | (dataset["saved"] == 1)
        | (dataset["commented"] == 1)
        | (dataset["shared"] == 1)
    )
    engaged = dataset[engaged_mask]
    non_engaged = dataset[~engaged_mask]

    if not engaged.empty:
        print(
            f"relevance engaged:       "
            f"{engaged['topic_relevance'].mean():.3f}"
        )
    else:
        print("relevance engaged:       N/A")

    if not non_engaged.empty:
        print(
            f"relevance non-engaged:   "
            f"{non_engaged['topic_relevance'].mean():.3f}"
        )
    else:
        print("relevance non-engaged:   N/A")

    if viewed > 0:
        print(
            f"(note: engagement rates above are over all rows; "
            f"viewed rows={viewed:,})"
        )
