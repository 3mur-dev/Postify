from __future__ import annotations

from dataclasses import dataclass
import random


@dataclass
class SimulatedUser:
    user_id: int
    archetype: str
    preferences: dict[int, float]

    activity_level: float
    session_length_mean: float
    exploration_rate: float
    engagement_bias: float


ARCHETYPES = {
    "developer": {
        "java": 0.95,
        "spring-boot": 0.90,
        "big-o-notation": 0.75,
        "cybersecurity": 0.45,
    },

    "gamer": {
        "video-games": 0.95,
        "game-development": 0.80,
        "java": 0.20,
    },

    "tech_enthusiast": {
        "smartphones": 0.90,
        "electric-vehicles": 0.75,
        "cybersecurity": 0.65,
    },

    "lifestyle": {
        "coffee": 0.90,
        "fitness-tracking": 0.75,
        "travel-delays": 0.30,
    },
}


def create_user(
        user_id: int,
        archetype: str,
        topics: list[dict],
        rng: random.Random | None = None,
) -> SimulatedUser:

    if archetype not in ARCHETYPES:
        raise ValueError(
            f"Unknown archetype: {archetype}"
        )

    rng = rng or random.Random()
    preferences = {}

    archetype_preferences = ARCHETYPES[archetype]

    topics_by_slug = {
        topic["slug"]: topic
        for topic in topics
    }

    for slug, base_interest in archetype_preferences.items():

        topic = topics_by_slug.get(slug)

        if topic is None:
            continue

        variation = rng.uniform(-0.10, 0.10)

        interest = max(
            0.0,
            min(1.0, base_interest + variation)
        )

        preferences[topic["id"]] = interest

    return SimulatedUser(
        user_id=user_id,
        archetype=archetype,
        preferences=preferences,
        activity_level=rng.uniform(0.4, 1.0),
        session_length_mean=rng.uniform(300, 1800),
        exploration_rate=rng.uniform(0.05, 0.30),
        engagement_bias=rng.uniform(0.30, 0.80),
    )
