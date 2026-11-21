from dataclasses import dataclass
from datetime import datetime
from enum import Enum


class SimulatedEventType(Enum):

    IMPRESSION = "IMPRESSION"

    VIEW_START = "VIEW_START"
    VIEW_END = "VIEW_END"

    MEDIA_START = "MEDIA_START"
    MEDIA_PROGRESS = "MEDIA_PROGRESS"
    MEDIA_COMPLETE = "MEDIA_COMPLETE"
    MEDIA_SKIP = "MEDIA_SKIP"

    LIKE = "LIKE"
    SAVE = "SAVE"
    COMMENT = "COMMENT"
    SHARE = "SHARE"

@dataclass
class SimulatedEvent:

    user_id: int
    post_id: int

    event_type: SimulatedEventType

    occurred_at: datetime

    duration_ms: int | None = None

    completion_ratio: float | None = None

    position_ms: int | None = None

    source: str = "FEED"

    session_id: str | None = None