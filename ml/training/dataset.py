from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq

from simulator.events import SimulatedEvent, SimulatedEventType
from simulator.posts import SimulatedPost
from simulator.sessions import SimulatedSession
from simulator.users import SimulatedUser


# ----------------------------------------------------------------------
# Feature / target separation for future ML training
#
# INPUT FEATURES — known BEFORE the interaction outcome:
#   user_id, post_id, session_id,
#   user_activity_level, user_session_length_mean,
#   user_exploration_rate, user_engagement_bias,
#   author_id, content_type, media_type, duration_ms, topic_count,
#   topic_relevance, post_created_at,
#   session_started_at, session_duration_seconds
#
# FUNNEL / CONTEXT OUTCOMES (not pre-interaction inputs when predicting
# deeper engagement from the same exposure):
#   impressed, viewed, impressed_at
#
# TARGETS — future outcomes of this exposure (never use as inputs for
# the same-row engagement model):
#   dwell_ms, completion_ratio, completed, skipped,
#   liked, saved, commented, shared
#
# Example — predicting LIKE:
#   features = INPUT_FEATURE_COLUMNS
#   target   = "liked"
#   do NOT include saved/commented/shared/dwell_ms/completion_* as inputs
# ----------------------------------------------------------------------

INPUT_FEATURE_COLUMNS = [
    "user_id",
    "post_id",
    "session_id",
    "user_activity_level",
    "user_session_length_mean",
    "user_exploration_rate",
    "user_engagement_bias",
    "author_id",
    "content_type",
    "media_type",
    "duration_ms",
    "topic_count",
    "topic_relevance",
    "post_created_at",
    "session_started_at",
    "session_duration_seconds",
    "history_impressions",
    "history_views",
    "history_likes",
    "history_saves",
    "history_comments",
    "history_shares",
    "history_like_rate",
    "history_save_rate",
    "history_comment_rate",
    "history_share_rate",
    "history_engagement_rate",
    "history_avg_dwell_ms",
    "history_avg_completion",
]

FUNNEL_COLUMNS = [
    "impressed",
    "viewed",
    "impressed_at",
]

TARGET_COLUMNS = [
    "dwell_ms",
    "completion_ratio",
    "completed",
    "skipped",
    "liked",
    "saved",
    "commented",
    "shared",
]


@dataclass
class UserHistory:
    impressions: int = 0
    views: int = 0
    likes: int = 0
    saves: int = 0
    comments: int = 0
    shares: int = 0

    total_dwell_ms: int = 0
    dwell_samples: int = 0

    total_completion: float = 0.0
    completion_samples: int = 0

    def to_features(self) -> dict:
        return {
            "history_impressions": self.impressions,
            "history_views": self.views,
            "history_likes": self.likes,
            "history_saves": self.saves,
            "history_comments": self.comments,
            "history_shares": self.shares,

            "history_like_rate": (
                self.likes / self.views
                if self.views > 0 else 0.0
            ),
            "history_save_rate": (
                self.saves / self.views
                if self.views > 0 else 0.0
            ),
            "history_comment_rate": (
                self.comments / self.views
                if self.views > 0 else 0.0
            ),
            "history_share_rate": (
                self.shares / self.views
                if self.views > 0 else 0.0
            ),

            "history_engagement_rate": (
                (
                    self.likes
                    + self.saves
                    + self.comments
                    + self.shares
                ) / self.views
                if self.views > 0 else 0.0
            ),

            "history_avg_dwell_ms": (
                self.total_dwell_ms / self.dwell_samples
                if self.dwell_samples > 0 else 0.0
            ),

            "history_avg_completion": (
                self.total_completion / self.completion_samples
                if self.completion_samples > 0 else 0.0
            ),
        }

    def apply_row(self, row: dict) -> None:
        """
        Add the CURRENT exposure to history.

        This must only be called AFTER the row has been built.
        """

        self.impressions += int(row["impressed"])

        if row["viewed"]:
            self.views += 1

        if row["liked"]:
            self.likes += 1

        if row["saved"]:
            self.saves += 1

        if row["commented"]:
            self.comments += 1

        if row["shared"]:
            self.shares += 1

        dwell_ms = row["dwell_ms"]

        if dwell_ms is not None and dwell_ms > 0:
            self.total_dwell_ms += int(dwell_ms)
            self.dwell_samples += 1

        completion = row["completion_ratio"]

        if completion is not None:
            self.total_completion += float(completion)
            self.completion_samples += 1


@dataclass
class DatasetBuilder:

    users: list[SimulatedUser]
    posts: list[SimulatedPost]
    sessions: list[SimulatedSession] | None = None

    _posts_by_id: dict[int, SimulatedPost] = field(init=False, repr=False)
    _users_by_id: dict[int, SimulatedUser] = field(init=False, repr=False)
    _sessions_by_id: dict[str, SimulatedSession] = field(init=False, repr=False)

    _user_histories: dict[int, UserHistory] = field(
        default_factory=dict,
        init=False,
        repr=False,
    )

    def __post_init__(self) -> None:
        self._posts_by_id = {
            post.post_id: post
            for post in self.posts
        }

        self._users_by_id = {
            user.user_id: user
            for user in self.users
        }

        self._sessions_by_id = {
            session.session_id: session
            for session in (self.sessions or [])
        }

        self._user_histories = {
            user.user_id: UserHistory()
            for user in self.users
        }

    def build(
        self,
        events: Iterable[SimulatedEvent]
    ) -> pd.DataFrame:

        rows = self.build_rows(events)
        if not rows:
            return pd.DataFrame()
        return pd.DataFrame(rows)

    def build_rows(
            self,
            events: Iterable[SimulatedEvent]
    ) -> list[dict]:
        """
        Convert an event batch into dataset rows.

        Historical features are calculated point-in-time:
        the current exposure is added to user history ONLY
        after the current row has been constructed.
        """

        events = list(events)

        if not events:
            return []

        grouped_events = self._group_events(events)

        exposure_groups = []

        for key, post_events in grouped_events.items():

            user_id, post_id, session_id = key

            impression_times = [
                event.occurred_at
                for event in post_events
                if event.event_type == SimulatedEventType.IMPRESSION
            ]

            if impression_times:
                timestamp = min(impression_times)
            else:
                timestamp = min(
                    event.occurred_at
                    for event in post_events
                )

            exposure_groups.append(
                (
                    timestamp,
                    user_id,
                    post_id,
                    session_id,
                    post_events,
                )
            )

        # Deterministic chronological ordering.
        exposure_groups.sort(
            key=lambda item: (
                item[0],
                item[1],
                item[2],
                item[3],
            )
        )

        rows: list[dict] = []

        for (
                timestamp,
                user_id,
                post_id,
                session_id,
                post_events,
        ) in exposure_groups:

            user = self._users_by_id.get(user_id)
            post = self._posts_by_id.get(post_id)

            if user is None or post is None:
                continue

            session = self._sessions_by_id.get(session_id)

            history = self._user_histories.setdefault(
                user_id,
                UserHistory(),
            )

            # IMPORTANT:
            # Capture history BEFORE applying this exposure.
            history_features = history.to_features()

            row = self._build_row(
                user=user,
                post=post,
                session=session,
                session_id=session_id,
                events=post_events,
                history_features=history_features,
            )

            rows.append(row)

            # IMPORTANT:
            # Current outcome enters history ONLY AFTER
            # the current row has been created.
            history.apply_row(row)

        return rows

    # ============================================================
    # GROUP EVENTS
    # ============================================================

    @staticmethod
    def _group_events(
        events: list[SimulatedEvent]
    ) -> dict[tuple[int, int, str], list[SimulatedEvent]]:

        grouped: dict[
            tuple[int, int, str],
            list[SimulatedEvent]
        ] = {}

        for event in events:

            key = (
                event.user_id,
                event.post_id,
                event.session_id
            )

            grouped.setdefault(key, []).append(event)

        return grouped

    # ============================================================
    # BUILD ONE TRAINING ROW
    # ============================================================

    def _build_row(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            session: SimulatedSession | None,
            session_id: str,
            events: list[SimulatedEvent],
            history_features: dict,
    ) -> dict:

        # Preserve generation order; do not re-sort to "fix" time bugs.
        event_types = {
            event.event_type
            for event in events
        }

        relevance = self._calculate_relevance(
            user=user,
            post=post
        )

        dwell_ms = self._get_dwell_time(events)
        is_video = post.content_type == "VIDEO"

        impression_times = [
            event.occurred_at
            for event in events
            if event.event_type == SimulatedEventType.IMPRESSION
        ]

        impressed_at = (
            min(impression_times)
            if impression_times
            else None
        )

        completed = int(
            is_video
            and SimulatedEventType.MEDIA_COMPLETE in event_types
        )

        skipped = int(
            is_video
            and SimulatedEventType.MEDIA_SKIP in event_types
        )

        # A video cannot be both completed and skipped.
        if completed and skipped:
            skipped = 0

        return {

            # ----------------------------------------------------
            # IDENTIFIERS / CONTEXT (inputs)
            # ----------------------------------------------------

            "user_id": user.user_id,
            "post_id": post.post_id,
            "session_id": session_id,

            # ----------------------------------------------------
            # USER FEATURES (inputs)
            # ----------------------------------------------------

            "user_activity_level": user.activity_level,
            "user_session_length_mean": user.session_length_mean,
            "user_exploration_rate": user.exploration_rate,
            "user_engagement_bias": user.engagement_bias,

            **history_features,

            # ----------------------------------------------------
            # POST FEATURES (inputs)
            # ----------------------------------------------------

            "author_id": post.author_id,
            "content_type": post.content_type,
            "media_type": post.media_type,
            "duration_ms": post.duration_ms,
            "topic_count": len(post.topics),
            "post_created_at": post.created_at,

            # ----------------------------------------------------
            # SESSION CONTEXT (inputs)
            # ----------------------------------------------------

            "session_started_at": (
                session.started_at if session is not None else None
            ),
            "session_duration_seconds": (
                session.duration_seconds if session is not None else None
            ),

            # ----------------------------------------------------
            # USER × POST FEATURES (inputs)
            # ----------------------------------------------------

            "topic_relevance": relevance,

            # ----------------------------------------------------
            # FUNNEL OUTCOMES
            # ----------------------------------------------------

            "impressed": int(
                SimulatedEventType.IMPRESSION in event_types
            ),
            "viewed": int(
                SimulatedEventType.VIEW_START in event_types
            ),
            "impressed_at": impressed_at,

            # ----------------------------------------------------
            # TARGETS / OBSERVED BEHAVIOR (not model inputs)
            # ----------------------------------------------------

            "dwell_ms": dwell_ms,

            "completion_ratio": (
                self._get_completion_ratio(events)
                if is_video
                else None
            ),

            "completed": completed,
            "skipped": skipped,

            "liked": int(
                SimulatedEventType.LIKE in event_types
            ),
            "saved": int(
                SimulatedEventType.SAVE in event_types
            ),
            "commented": int(
                SimulatedEventType.COMMENT in event_types
            ),
            "shared": int(
                SimulatedEventType.SHARE in event_types
            ),
        }

    # ============================================================
    # RELEVANCE
    # ============================================================

    @staticmethod
    def _calculate_relevance(
        user: SimulatedUser,
        post: SimulatedPost
    ) -> float:

        interests = []

        for topic_id in post.topics:

            interest = user.preferences.get(topic_id)

            if interest is not None:
                interests.append(interest)

        if not interests:
            return 0.0

        return sum(interests) / len(interests)

    # ============================================================
    # DWELL TIME
    # ============================================================

    @staticmethod
    def _get_dwell_time(
        events: list[SimulatedEvent]
    ) -> int:

        dwell_times = [
            event.duration_ms
            for event in events
            if (
                event.event_type == SimulatedEventType.VIEW_END
                and event.duration_ms is not None
            )
        ]

        if not dwell_times:
            return 0

        return max(dwell_times)

    # ============================================================
    # COMPLETION (VIDEO only — caller gates on content_type)
    # ============================================================

    @staticmethod
    def _get_completion_ratio(
        events: list[SimulatedEvent]
    ) -> float | None:

        # Prefer VIEW_END — it carries the final dwell-consistent ratio.
        for event in reversed(events):
            if (
                event.event_type == SimulatedEventType.VIEW_END
                and event.completion_ratio is not None
            ):
                return event.completion_ratio

        completion_values = [
            event.completion_ratio
            for event in events
            if event.completion_ratio is not None
        ]

        if not completion_values:
            return None

        return max(completion_values)


class ParquetBatchWriter:
    """
    Stream dataset rows to Parquet in configurable batches.

    Avoids holding the full simulation result set in memory.
    """

    def __init__(
            self,
            path: str | Path,
            batch_size: int = 10_000,
    ):
        self.path = Path(path)
        self.batch_size = max(1, batch_size)
        self._buffer: list[dict] = []
        self._writer: pq.ParquetWriter | None = None
        self.rows_written = 0

    def add_rows(self, rows: list[dict]) -> None:
        if not rows:
            return
        self._buffer.extend(rows)
        while len(self._buffer) >= self.batch_size:
            chunk = self._buffer[: self.batch_size]
            del self._buffer[: self.batch_size]
            self._write_chunk(chunk)

    def close(self) -> int:
        if self._buffer:
            self._write_chunk(self._buffer)
            self._buffer = []
        if self._writer is not None:
            self._writer.close()
            self._writer = None
        return self.rows_written

    def _write_chunk(self, rows: list[dict]) -> None:
        frame = pd.DataFrame(rows)
        table = pa.Table.from_pandas(frame, preserve_index=False)

        self.path.parent.mkdir(parents=True, exist_ok=True)

        if self._writer is None:
            self._writer = pq.ParquetWriter(
                where=str(self.path),
                schema=table.schema,
                compression="zstd",
            )

        self._writer.write_table(table)
        self.rows_written += len(rows)
