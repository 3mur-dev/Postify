from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
import random

from simulator.users import SimulatedUser


@dataclass
class SimulatedSession:
    session_id: str
    user_id: int
    started_at: datetime
    duration_seconds: float
    interactions: int = 0


def create_session(
        user: SimulatedUser,
        started_at: datetime,
        rng: random.Random | None = None,
        session_id: str | None = None,
) -> SimulatedSession:
    """
    Duration is driven by session_length_mean + noise.
    Frequency of sessions is handled by the caller via activity_level.
    """

    rng = rng or random.Random()

    duration = rng.gauss(
        user.session_length_mean,
        user.session_length_mean * 0.30,
    )

    # Realistic bounds: at least 30s, at most ~90 minutes.
    duration = max(30.0, min(duration, 5_400.0))

    if session_id is None:
        # Deterministic when rng is seeded via SimulationEngine counters.
        session_id = f"s-{user.user_id}-{int(started_at.timestamp() * 1000)}"

    return SimulatedSession(
        session_id=session_id,
        user_id=user.user_id,
        started_at=started_at,
        duration_seconds=duration,
    )
