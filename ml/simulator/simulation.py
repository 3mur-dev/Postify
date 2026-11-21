from __future__ import annotations

from bisect import bisect_right
from datetime import datetime, timedelta
import random

from simulator.behavior import BehaviorModel
from simulator.config import SimulationConfig
from simulator.feed import select_posts_for_session
from simulator.posts import create_post
from simulator.sessions import create_session
from simulator.simulator import InteractionSimulator
from simulator.users import ARCHETYPES, create_user


class SimulationEngine:
    """
    Config-driven user / post / session / event generation.

    Reuses create_user, create_post, create_session, feed selection,
    and InteractionSimulator. Dataset sizes come only from SimulationConfig.
    """

    def __init__(
            self,
            topics: list[dict],
            behavior_model: BehaviorModel,
            config: SimulationConfig | None = None,
            simulation_now: datetime | None = None,
    ):
        self.topics = topics
        self.behavior_model = behavior_model
        self.config = config or SimulationConfig()
        self.rng = random.Random(self.config.seed)
        self.interaction_simulator = InteractionSimulator(
            behavior_model,
            rng=self.rng,
        )
        self.simulation_now = simulation_now or datetime(2026, 8, 1, 12, 0, 0)
        self._session_seq = 0

        # Built after create_posts for fast eligibility lookups.
        self._posts_by_created: list = []
        self._post_created_keys: list[float] = []

    # ============================================================
    # USERS
    # ============================================================

    def create_users(self, count: int | None = None):
        count = self.config.user_count if count is None else count
        users = []
        archetypes = list(ARCHETYPES.keys())

        for user_id in range(1, count + 1):
            # Cycle archetypes, then per-user preference noise via create_user.
            archetype = archetypes[(user_id - 1) % len(archetypes)]
            user = create_user(
                user_id=user_id,
                archetype=archetype,
                topics=self.topics,
                rng=self.rng,
            )
            users.append(user)

        return users

    # ============================================================
    # POSTS
    # ============================================================

    def create_posts(
            self,
            count: int | None = None,
            author_ids: list[int] | None = None,
    ):
        count = self.config.post_count if count is None else count
        if not author_ids:
            raise ValueError("author_ids is required to create posts.")

        author_weights = self._author_posting_weights(author_ids)
        content_types, content_weights = self._content_type_distribution()

        sim_start = self.simulation_now - timedelta(days=self.config.simulation_days)
        # Posts span history before and during the simulation window.
        post_window_start = sim_start - timedelta(days=self.config.post_history_days)
        post_window_end = self.simulation_now - timedelta(hours=1)

        posts = []

        for offset in range(count):
            post_id = 1001 + offset

            topic_count = self.rng.choices(
                [1, 2, 3],
                weights=[0.55, 0.35, 0.10],
                k=1,
            )[0]

            selected_topics = self.rng.sample(
                self.topics,
                k=min(topic_count, len(self.topics)),
            )
            topic_ids = [topic["id"] for topic in selected_topics]

            author_id = self.rng.choices(
                author_ids,
                weights=author_weights,
                k=1,
            )[0]

            content_type = self.rng.choices(
                content_types,
                weights=content_weights,
                k=1,
            )[0]

            if content_type == "TEXT":
                media_type = None
                duration_ms = None
            elif content_type == "IMAGE":
                media_type = "IMAGE"
                duration_ms = None
            else:
                media_type = "VIDEO"
                duration_ms = self.rng.randint(5_000, 90_000)

            # Spread created_at across the post window (not one timestamp).
            fraction = self.rng.random()
            created_at = post_window_start + (post_window_end - post_window_start) * fraction

            post = create_post(
                post_id=post_id,
                topics=topic_ids,
                author_id=author_id,
                content_type=content_type,
                media_type=media_type,
                duration_ms=duration_ms,
                created_at=created_at,
            )
            posts.append(post)

        posts.sort(key=lambda post: post.created_at)
        self._index_posts(posts)
        return posts

    def _author_posting_weights(self, author_ids: list[int]) -> list[float]:
        """
        Uneven but non-extreme author activity via Pareto draws.
        """
        shape = max(0.5, self.config.author_activity_shape)
        weights = []
        for _ in author_ids:
            # Pareto(xm=1) → heavier tail for lower shape.
            weight = (self.rng.paretovariate(shape)) ** 0.85
            weights.append(max(0.05, weight))
        return weights

    def _content_type_distribution(self) -> tuple[list[str], list[float]]:
        weights_map = self.config.content_type_weights
        types = list(weights_map.keys())
        weights = [max(0.0, float(weights_map[t])) for t in types]
        total = sum(weights) or 1.0
        return types, [w / total for w in weights]

    def _index_posts(self, posts: list) -> None:
        self._posts_by_created = list(posts)
        self._post_created_keys = [
            post.created_at.timestamp()
            for post in self._posts_by_created
        ]

    def eligible_posts_for_time(self, when: datetime, posts: list | None = None) -> list:
        """
        Posts with created_at <= when, via binary search when indexed.
        """
        if posts is not None and posts is not self._posts_by_created:
            return [post for post in posts if post.created_at <= when]

        if not self._posts_by_created:
            return []

        idx = bisect_right(self._post_created_keys, when.timestamp())
        return self._posts_by_created[:idx]

    # ============================================================
    # SESSIONS
    # ============================================================

    def create_sessions(
            self,
            users: list,
            sessions_per_user: int | None = None,
            session_count: int | None = None,
    ):
        """
        Prefer total session_count from config, allocated by activity_level.

        sessions_per_user remains supported for the old fixed-per-user path.
        """
        if sessions_per_user is not None and session_count is None:
            # Backward-compatible: exactly N sessions per user.
            sessions = []
            for user in users:
                for _ in range(sessions_per_user):
                    sessions.append(self._make_session(user))
            return sessions

        total = (
            self.config.session_count
            if session_count is None
            else session_count
        )
        if total <= 0 or not users:
            return []

        allocations = self._allocate_sessions_by_activity(users, total)
        sessions = []

        for user, n_sessions in zip(users, allocations):
            for _ in range(n_sessions):
                sessions.append(self._make_session(user))

        # Chronological order helps streaming locality; not used to "fix" events.
        sessions.sort(key=lambda session: session.started_at)
        return sessions

    def _allocate_sessions_by_activity(
            self,
            users: list,
            total: int,
    ) -> list[int]:
        exponent = self.config.activity_session_exponent
        weights = [
            max(0.05, user.activity_level) ** exponent
            for user in users
        ]
        weight_sum = sum(weights)

        # Largest-remainder method for integer allocation.
        raw = [total * (w / weight_sum) for w in weights]
        base = [int(x) for x in raw]
        remainder = total - sum(base)
        fractional = sorted(
            enumerate(raw),
            key=lambda item: item[1] - int(item[1]),
            reverse=True,
        )
        for i in range(remainder):
            base[fractional[i % len(fractional)][0]] += 1

        return base

    def _make_session(self, user):
        days_ago = self.rng.uniform(0, self.config.simulation_days)
        started_at = self.simulation_now - timedelta(days=days_ago)

        self._session_seq += 1
        session_id = f"s-{self.config.seed:04d}-{self._session_seq:08d}"

        return create_session(
            user=user,
            started_at=started_at,
            rng=self.rng,
            session_id=session_id,
        )

    # ============================================================
    # SESSION SIMULATION
    # ============================================================

    def simulate_session(
            self,
            user,
            session,
            posts,
    ):
        """
        Generate events strictly inside the session window.

        Feed selection does not use future engagement outcomes.
        Timestamps increase monotonically; no post-hoc sorting fixes.
        """

        events = []

        session_end = (
            session.started_at
            + timedelta(seconds=session.duration_seconds)
        )
        current_time = session.started_at

        eligible_posts = self.eligible_posts_for_time(
            session.started_at,
            posts=posts if not self._posts_by_created else None,
        )

        if not eligible_posts:
            return events

        feed_size = self.rng.randint(
            self.config.min_feed_size,
            self.config.max_feed_size,
        )
        candidate_pool_size = min(
            len(eligible_posts),
            max(
                feed_size,
                int(feed_size * self.config.candidate_pool_multiplier),
            ),
        )

        session_posts = select_posts_for_session(
            user=user,
            posts=eligible_posts,
            count=feed_size,
            rng=self.rng,
            candidate_pool_size=candidate_pool_size,
        )

        seen_post_ids: set[int] = set()

        for post in session_posts:

            if current_time >= session_end:
                break

            if post.post_id in seen_post_ids:
                continue

            seen_post_ids.add(post.post_id)

            post_events = (
                self.interaction_simulator
                .simulate_post_interaction(
                    user=user,
                    post=post,
                    session=session,
                    timestamp=current_time,
                )
            )

            if not post_events:
                continue

            if any(
                event.occurred_at < session.started_at
                or event.occurred_at > session_end
                for event in post_events
            ):
                break

            for earlier, later in zip(post_events, post_events[1:]):
                if later.occurred_at < earlier.occurred_at:
                    raise RuntimeError(
                        "Non-monotonic event timestamps generated "
                        f"for user={user.user_id} post={post.post_id}."
                    )

            events.extend(post_events)
            session.interactions += 1

            current_time = post_events[-1].occurred_at
            current_time += timedelta(
                milliseconds=self.rng.randint(100, 1500)
            )

        return events
