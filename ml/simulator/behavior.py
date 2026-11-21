import math

from simulator.users import SimulatedUser
from simulator.posts import SimulatedPost


class BehaviorModel:
    """
    Probabilistic behavior driven by relevance → attention → engagement.

    Probabilities use logistic / noisy blends so relevance is predictive
    but not deterministic (no hard thresholds like like = relevance > X).
    """

    def calculate_relevance(
            self,
            user: SimulatedUser,
            post: SimulatedPost
    ) -> float:

        if not post.topics:
            return 0.0

        scores = [
            user.preferences.get(topic_id, 0.0)
            for topic_id in post.topics
        ]

        return sum(scores) / len(scores)

    def calculate_effective_relevance(
            self,
            user: SimulatedUser,
            post: SimulatedPost
    ) -> float:

        relevance = self.calculate_relevance(user, post)

        unknown_topics = [
            topic_id
            for topic_id in post.topics
            if topic_id not in user.preferences
        ]

        # Exploratory users get a small boost on unfamiliar topics.
        if unknown_topics:
            relevance += user.exploration_rate * 0.25

        return max(0.0, min(1.0, relevance))

    def probability_of_view(
            self,
            user: SimulatedUser,
            post: SimulatedPost
    ) -> float:

        relevance = self.calculate_effective_relevance(user, post)

        score = (
            (relevance - 0.45) * 4.5
            + user.engagement_bias * 0.8
            + user.activity_level * 0.6
            - 0.15
        )

        return self._clamp(
            0.05 + 0.85 * self._sigmoid(score)
        )

    def probability_of_like(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            attention: float | None
    ) -> float:

        return self._engagement_probability(
            user=user,
            post=post,
            attention=attention,
            base=0.015,
            relevance_w=2.2,
            attention_w=1.4,
            engagement_w=1.0,
            exploration_w=-0.6,
            content_boost=self._content_boost(post, like=True),
            maximum=0.22
        )

    def probability_of_save(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            attention: float | None
    ) -> float:
        """Sparse. Higher when relevance and attention are both strong."""

        return self._engagement_probability(
            user=user,
            post=post,
            attention=attention,
            base=0.004,
            relevance_w=2.6,
            attention_w=1.6,
            engagement_w=0.9,
            exploration_w=-0.3,
            content_boost=self._content_boost(post, save=True),
            maximum=0.08
        )

    def probability_of_comment(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            attention: float | None
    ) -> float:
        """Rarer than likes/saves."""

        return self._engagement_probability(
            user=user,
            post=post,
            attention=attention,
            base=0.0015,
            relevance_w=2.0,
            attention_w=1.8,
            engagement_w=1.2,
            exploration_w=0.1,
            content_boost=self._content_boost(post, comment=True),
            maximum=0.035
        )

    def probability_of_share(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            attention: float | None
    ) -> float:
        """Rarest engagement action."""

        return self._engagement_probability(
            user=user,
            post=post,
            attention=attention,
            base=0.001,
            relevance_w=1.8,
            attention_w=2.0,
            engagement_w=1.1,
            exploration_w=0.2,
            content_boost=self._content_boost(post, share=True),
            maximum=0.025
        )

    def _engagement_probability(
            self,
            user: SimulatedUser,
            post: SimulatedPost,
            attention: float | None,
            base: float,
            relevance_w: float,
            attention_w: float,
            engagement_w: float,
            exploration_w: float,
            content_boost: float,
            maximum: float
    ) -> float:

        relevance = self.calculate_effective_relevance(user, post)
        attention_value = (
            attention if attention is not None else 0.0
        )

        score = (
            relevance_w * (relevance - 0.5)
            + attention_w * (attention_value - 0.45)
            + engagement_w * (user.engagement_bias - 0.5)
            + exploration_w * (user.exploration_rate - 0.15)
            + content_boost
        )

        probability = base + (maximum - base) * self._sigmoid(score)

        return self._clamp(probability, minimum=base * 0.5, maximum=maximum)

    @staticmethod
    def _content_boost(
            post: SimulatedPost,
            like: bool = False,
            save: bool = False,
            comment: bool = False,
            share: bool = False
    ) -> float:

        content_type = post.content_type

        if like:
            if content_type == "IMAGE":
                return 0.15
            if content_type == "VIDEO":
                return 0.10
            return 0.05

        if save:
            if content_type == "TEXT":
                return 0.20
            if content_type == "IMAGE":
                return 0.10
            return 0.05

        if comment:
            if content_type == "TEXT":
                return 0.25
            if content_type == "VIDEO":
                return 0.05
            return 0.0

        if share:
            if content_type == "VIDEO":
                return 0.20
            if content_type == "IMAGE":
                return 0.15
            return 0.0

        return 0.0

    @staticmethod
    def _sigmoid(value: float) -> float:

        return 1.0 / (1.0 + math.exp(-value))

    @staticmethod
    def _clamp(
            value: float,
            minimum: float = 0.0,
            maximum: float = 1.0
    ) -> float:

        return max(minimum, min(maximum, value))
