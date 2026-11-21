from __future__ import annotations

from datetime import datetime
from typing import Annotated

from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
)


class CandidateFeatures(BaseModel):
    model_config = ConfigDict(
        extra="forbid"
    )

    post_id: int = Field(
        gt=0
    )

    post_created_at: datetime

    user_activity_level: float
    user_session_length_mean: float
    user_exploration_rate: float
    user_engagement_bias: float

    history_impressions: int = Field(
        ge=0
    )

    history_views: int = Field(
        ge=0
    )

    history_likes: int = Field(
        ge=0
    )

    history_saves: int = Field(
        ge=0
    )

    history_comments: int = Field(
        ge=0
    )

    history_shares: int = Field(
        ge=0
    )

    history_like_rate: float = Field(
        ge=0
    )

    history_save_rate: float = Field(
        ge=0
    )

    history_comment_rate: float = Field(
        ge=0
    )

    history_share_rate: float = Field(
        ge=0
    )

    history_engagement_rate: float = Field(
        ge=0
    )

    history_avg_dwell_ms: float = Field(
        ge=0
    )

    history_avg_completion: float = Field(
        ge=0,
        le=1,
    )

    duration_ms: float | None = Field(
        default=None,
        ge=0,
    )

    topic_count: int = Field(
        ge=0
    )

    topic_relevance: float = Field(
        ge=0,
        le=1,
    )

    content_type: str

    media_type: str | None = None


class RankRequest(BaseModel):
    model_config = ConfigDict(
        extra="forbid"
    )

    session_started_at: datetime

    candidates: Annotated[
        list[CandidateFeatures],
        Field(
            min_length=1,
            max_length=200,
        ),
    ]


class RankedCandidate(BaseModel):
    post_id: int

    score: float

    rank: int


class RankResponse(BaseModel):
    model_version: str

    candidate_count: int

    ranked_candidates: list[
        RankedCandidate
    ]