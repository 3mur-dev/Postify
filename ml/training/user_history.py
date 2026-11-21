# history.py
from __future__ import annotations
from dataclasses import dataclass, field
from collections import defaultdict

@dataclass
class UserHistory:
    impressions: int = 0
    views: int = 0
    likes: int = 0
    saves: int = 0
    comments: int = 0
    shares: int = 0

    total_dwell_ms: int = 0
    total_video_views: int = 0
    total_completion: float = 0.0

    topic_interactions: dict[int, float] = field(
        default_factory=lambda: defaultdict(float)
    )

    def avg_dwell_ms(self) -> float:
        if self.views == 0:
            return 0.0
        return self.total_dwell_ms / self.views

    def avg_completion(self) -> float:
        if self.total_video_views == 0:
            return 0.0
        return self.total_completion / self.total_video_views


class UserHistoryBuilder:
    def __init__(self):
        self.histories: dict[int, UserHistory] = defaultdict(UserHistory)

    def get_history(self, user_id: int) -> UserHistory:
        return self.histories[user_id]

    def update(
            self,
            user_id: int,
            *,
            impressed: bool = False,
            viewed: bool = False,
            liked: bool = False,
            saved: bool = False,
            commented: bool = False,
            shared: bool = False,
            dwell_ms: int | None = None,
            completion_ratio: float | None = None,
            is_video: bool = False,
            topic_ids: list[int] | None = None,
    ) -> None:
        history = self.histories[user_id]

        if impressed: history.impressions += 1
        if viewed: history.views += 1
        if liked: history.likes += 1
        if saved: history.saves += 1
        if commented: history.comments += 1
        if shared: history.shares += 1

        if dwell_ms is not None:
            history.total_dwell_ms += max(0, dwell_ms)

        if is_video and viewed:
            history.total_video_views += 1
            if completion_ratio is not None:
                history.total_completion += completion_ratio

        if topic_ids:
            for topic_id in topic_ids:
                weight = 1.0
                if liked: weight += 3.0
                if saved: weight += 4.0
                if commented: weight += 5.0
                if shared: weight += 2.0
                if viewed: weight += 0.5
                history.topic_interactions[topic_id] += weight

    def features(self, user_id: int) -> dict[str, float]:
        history = self.histories[user_id]
        return {
            "history_impressions": history.impressions,
            "history_views": history.views,
            "history_likes": history.likes,
            "history_saves": history.saves,
            "history_comments": history.comments,
            "history_shares": history.shares,
            "history_like_rate": history.likes / history.views if history.views else 0.0,
            "history_save_rate": history.saves / history.views if history.views else 0.0,
            "history_comment_rate": history.comments / history.views if history.views else 0.0,
            "history_share_rate": history.shares / history.views if history.views else 0.0,
            "history_avg_dwell_ms": history.avg_dwell_ms(),
            "history_avg_completion": history.avg_completion(),
            "history_engagement_rate": (
                (history.likes + history.saves + history.comments + history.shares) / history.views
                if history.views else 0.0
            ),
        }
