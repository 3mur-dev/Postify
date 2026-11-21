from datetime import datetime, timedelta
import random

from simulator.users import SimulatedUser
from simulator.posts import SimulatedPost
from simulator.behavior import BehaviorModel
from simulator.events import (
    SimulatedEvent,
    SimulatedEventType
)
from simulator.sessions import SimulatedSession


class InteractionSimulator:
    """
    Simulates one user × post exposure inside a session.

    Event order:
        IMPRESSION → (optional) VIEW_START → consumption → VIEW_END
        → optional engagement (LIKE / SAVE / COMMENT / SHARE)

    VIDEO also emits MEDIA_* events. Completion applies only to VIDEO.
    """

    # Near-full watch counts as completed; otherwise the watch is a skip.
    COMPLETE_THRESHOLD = 0.95

    def __init__(
            self,
            behavior_model: BehaviorModel,
            rng: random.Random | None = None
    ):
        self.behavior_model = behavior_model
        self.rng = rng or random.Random()

    # ============================================================
    # PUBLIC API
    # ============================================================

    def simulate_post_interaction(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            session: SimulatedSession,
            timestamp: datetime
    ) -> list[SimulatedEvent]:

        self._validate_post_media(post)

        events: list[SimulatedEvent] = []
        current_time = timestamp

        # Posts must already exist at interaction time.
        if post.created_at > current_time:
            return events

        # --------------------------------------------------------
        # 1. IMPRESSION
        # --------------------------------------------------------

        events.append(
            self._event(
                user=user,
                post=post,
                session=session,
                event_type=SimulatedEventType.IMPRESSION,
                timestamp=current_time
            )
        )

        # --------------------------------------------------------
        # 2. Decide whether the user views the post
        # --------------------------------------------------------

        view_probability = self._clamp(
            self.behavior_model.probability_of_view(
                user,
                post
            )
        )

        if self.rng.random() >= view_probability:
            return events

        current_time += timedelta(
            milliseconds=self.rng.randint(200, 1800)
        )

        events.append(
            self._event(
                user=user,
                post=post,
                session=session,
                event_type=SimulatedEventType.VIEW_START,
                timestamp=current_time
            )
        )

        # --------------------------------------------------------
        # 3. Consume content
        # --------------------------------------------------------

        attention = 0.0

        if post.content_type == "VIDEO":
            current_time, attention = self._simulate_video(
                user=user,
                post=post,
                session=session,
                timestamp=current_time,
                events=events
            )

        elif post.content_type in ("IMAGE", "TEXT"):
            current_time, attention = self._simulate_static_content(
                user=user,
                post=post,
                session=session,
                timestamp=current_time,
                events=events
            )

        else:
            raise ValueError(
                f"Unsupported content type: {post.content_type}"
            )

        # --------------------------------------------------------
        # 4. Engagement (only after a meaningful view / VIEW_END)
        # --------------------------------------------------------

        self._append_engagement_events(
            user=user,
            post=post,
            session=session,
            events=events,
            current_time=current_time,
            attention=attention
        )

        return events

    # ============================================================
    # VIDEO
    # ============================================================

    def _simulate_video(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            session: SimulatedSession,
            timestamp: datetime,
            events: list[SimulatedEvent]
    ) -> tuple[datetime, float]:

        duration_ms = post.duration_ms
        assert duration_ms is not None and duration_ms > 0

        completion = self._simulate_completion(user, post)

        watched_ms = max(
            250,
            int(round(duration_ms * completion))
        )
        watched_ms = min(watched_ms, duration_ms)

        # Keep completion_ratio consistent with dwell / duration.
        completion = watched_ms / duration_ms

        current_time = timestamp

        events.append(
            self._event(
                user=user,
                post=post,
                session=session,
                event_type=SimulatedEventType.MEDIA_START,
                timestamp=current_time,
                position_ms=0
            )
        )

        progress_interval_ms = self._progress_interval(duration_ms)
        progress_position = progress_interval_ms

        while progress_position < watched_ms:
            current_time += timedelta(milliseconds=progress_interval_ms)
            progress_ratio = progress_position / duration_ms

            events.append(
                self._event(
                    user=user,
                    post=post,
                    session=session,
                    event_type=SimulatedEventType.MEDIA_PROGRESS,
                    timestamp=current_time,
                    position_ms=progress_position,
                    duration_ms=progress_position,
                    completion_ratio=progress_ratio
                )
            )

            progress_position += progress_interval_ms

        current_time = timestamp + timedelta(milliseconds=watched_ms)

        if completion >= self.COMPLETE_THRESHOLD:
            events.append(
                self._event(
                    user=user,
                    post=post,
                    session=session,
                    event_type=SimulatedEventType.MEDIA_COMPLETE,
                    timestamp=current_time,
                    position_ms=watched_ms,
                    completion_ratio=completion
                )
            )
        else:
            events.append(
                self._event(
                    user=user,
                    post=post,
                    session=session,
                    event_type=SimulatedEventType.MEDIA_SKIP,
                    timestamp=current_time,
                    position_ms=watched_ms,
                    completion_ratio=completion
                )
            )

        events.append(
            self._event(
                user=user,
                post=post,
                session=session,
                event_type=SimulatedEventType.VIEW_END,
                timestamp=current_time,
                duration_ms=watched_ms,
                position_ms=watched_ms,
                completion_ratio=completion
            )
        )

        return current_time, completion

    # ============================================================
    # IMAGE / TEXT
    # ============================================================

    def _simulate_static_content(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            session: SimulatedSession,
            timestamp: datetime,
            events: list[SimulatedEvent]
    ) -> tuple[datetime, float]:

        dwell_ms = self._simulate_dwell_time(user, post)

        current_time = timestamp + timedelta(milliseconds=dwell_ms)

        # No completion_ratio for non-video content.
        events.append(
            self._event(
                user=user,
                post=post,
                session=session,
                event_type=SimulatedEventType.VIEW_END,
                timestamp=current_time,
                duration_ms=dwell_ms,
                position_ms=dwell_ms
            )
        )

        # Map dwell to an attention signal in [0, 1] for engagement probs.
        expected_ms = 6_000 if post.content_type == "IMAGE" else 8_000
        attention = self._clamp(dwell_ms / expected_ms)

        return current_time, attention

    # ============================================================
    # ENGAGEMENT
    # ============================================================

    def _append_engagement_events(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            session: SimulatedSession,
            events: list[SimulatedEvent],
            current_time: datetime,
            attention: float
    ) -> datetime:
        """
        Emit at most one of each engagement type.
        Requires VIEW_END already present in `events`.
        """

        has_view_end = any(
            event.event_type == SimulatedEventType.VIEW_END
            for event in events
        )

        if not has_view_end:
            return current_time

        engagement_specs = [
            (
                SimulatedEventType.LIKE,
                self.behavior_model.probability_of_like(
                    user, post, attention
                ),
                (300, 2500),
            ),
            (
                SimulatedEventType.SAVE,
                self.behavior_model.probability_of_save(
                    user, post, attention
                ),
                (300, 2000),
            ),
            (
                SimulatedEventType.COMMENT,
                self.behavior_model.probability_of_comment(
                    user, post, attention
                ),
                (800, 5000),
            ),
            (
                SimulatedEventType.SHARE,
                self.behavior_model.probability_of_share(
                    user, post, attention
                ),
                (400, 3000),
            ),
        ]

        emitted: set[SimulatedEventType] = set()

        for event_type, probability, delay_range in engagement_specs:

            if event_type in emitted:
                continue

            probability = self._clamp(probability)

            if self.rng.random() >= probability:
                continue

            current_time += timedelta(
                milliseconds=self.rng.randint(*delay_range)
            )

            events.append(
                self._event(
                    user=user,
                    post=post,
                    session=session,
                    event_type=event_type,
                    timestamp=current_time
                )
            )

            emitted.add(event_type)

        return current_time

    # ============================================================
    # BEHAVIOR HELPERS
    # ============================================================

    def _simulate_completion(
            self,
            user: SimulatedUser,
            post: SimulatedPost
    ) -> float:

        relevance = (
            self.behavior_model
            .calculate_effective_relevance(user, post)
        )

        noise = self.rng.gauss(0.0, 0.15)

        raw = (
            0.18
            + relevance * 0.70
            + user.engagement_bias * 0.22
            + noise
        )

        return self._clamp(raw, minimum=0.01, maximum=1.0)

    def _simulate_dwell_time(
            self,
            user: SimulatedUser,
            post: SimulatedPost
    ) -> int:

        relevance = (
            self.behavior_model
            .calculate_effective_relevance(user, post)
        )

        if post.content_type == "IMAGE":
            base_ms = self.rng.randint(1500, 8000)
        else:
            base_ms = self.rng.randint(2500, 12000)

        relevance_multiplier = 0.50 + relevance * 1.50
        engagement_multiplier = 0.75 + user.engagement_bias * 0.75
        noise_multiplier = self.rng.uniform(0.75, 1.25)

        dwell_ms = (
            base_ms
            * relevance_multiplier
            * engagement_multiplier
            * noise_multiplier
        )

        return max(500, int(dwell_ms))

    # ============================================================
    # HELPERS
    # ============================================================

    @staticmethod
    def _validate_post_media(post: SimulatedPost) -> None:

        if post.content_type == "VIDEO":
            if post.media_type != "VIDEO":
                raise ValueError(
                    f"VIDEO post {post.post_id} must have media_type=VIDEO."
                )
            if post.duration_ms is None or post.duration_ms <= 0:
                raise ValueError(
                    f"VIDEO post {post.post_id} must have duration_ms > 0."
                )

        elif post.content_type == "IMAGE":
            if post.media_type != "IMAGE":
                raise ValueError(
                    f"IMAGE post {post.post_id} must have media_type=IMAGE."
                )
            if post.duration_ms is not None:
                raise ValueError(
                    f"IMAGE post {post.post_id} must have duration_ms=None."
                )

        elif post.content_type == "TEXT":
            if post.media_type is not None:
                raise ValueError(
                    f"TEXT post {post.post_id} must have media_type=None."
                )
            if post.duration_ms is not None:
                raise ValueError(
                    f"TEXT post {post.post_id} must have duration_ms=None."
                )

    def _progress_interval(self, duration_ms: int) -> int:

        if duration_ms <= 10_000:
            return 2_000

        if duration_ms <= 60_000:
            return 5_000

        return 10_000

    def _event(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            session: SimulatedSession,
            event_type: SimulatedEventType,
            timestamp: datetime,
            duration_ms: int | None = None,
            completion_ratio: float | None = None,
            position_ms: int | None = None
    ) -> SimulatedEvent:

        return SimulatedEvent(
            user_id=user.user_id,
            post_id=post.post_id,
            event_type=event_type,
            occurred_at=timestamp,
            duration_ms=duration_ms,
            completion_ratio=completion_ratio,
            position_ms=position_ms,
            source="FEED",
            session_id=session.session_id
        )

    @staticmethod
    def _clamp(
            value: float,
            minimum: float = 0.0,
            maximum: float = 1.0
    ) -> float:

        return max(minimum, min(maximum, value))
