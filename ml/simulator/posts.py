from dataclasses import dataclass
from datetime import datetime
from enum import Enum


class ContentType(Enum):
    TEXT = "TEXT"
    IMAGE = "IMAGE"
    VIDEO = "VIDEO"


class MediaType(Enum):
    IMAGE = "IMAGE"
    VIDEO = "VIDEO"


@dataclass
class SimulatedPost:
    post_id: int
    topics: list[int]
    author_id: int
    content_type: str
    media_type: str | None
    duration_ms: int | None
    created_at: datetime


def create_post(
        post_id: int,
        topics: list[int],
        author_id: int,
        content_type: str,
        created_at: datetime,
        media_type: str | None = None,
        duration_ms: int | None = None
) -> SimulatedPost:

    if not topics:
        raise ValueError(
            "A simulated post must have at least one topic."
        )

    validate_media_semantics(
        content_type=content_type,
        media_type=media_type,
        duration_ms=duration_ms
    )

    return SimulatedPost(
        post_id=post_id,
        topics=topics,
        author_id=author_id,
        content_type=content_type,
        media_type=media_type,
        duration_ms=duration_ms,
        created_at=created_at
    )


def validate_media_semantics(
        content_type: str,
        media_type: str | None,
        duration_ms: int | None
) -> None:
    """
    VIDEO  → media_type=VIDEO, duration_ms > 0
    IMAGE  → media_type=IMAGE, duration_ms=None
    TEXT   → media_type=None,  duration_ms=None
    """

    if content_type == "VIDEO":
        if media_type != "VIDEO":
            raise ValueError(
                "VIDEO posts require media_type='VIDEO'."
            )
        if duration_ms is None or duration_ms <= 0:
            raise ValueError(
                "VIDEO posts require duration_ms > 0."
            )

    elif content_type == "IMAGE":
        if media_type != "IMAGE":
            raise ValueError(
                "IMAGE posts require media_type='IMAGE'."
            )
        if duration_ms is not None:
            raise ValueError(
                "IMAGE posts require duration_ms=None."
            )

    elif content_type == "TEXT":
        if media_type is not None:
            raise ValueError(
                "TEXT posts require media_type=None."
            )
        if duration_ms is not None:
            raise ValueError(
                "TEXT posts require duration_ms=None."
            )

    else:
        raise ValueError(
            f"Unsupported content_type: {content_type}"
        )
