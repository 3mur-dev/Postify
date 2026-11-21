from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path


@dataclass(frozen=True)
class SimulationConfig:
    """
    Controls simulator scale and realism knobs.

    Development and training use the same code path; only this config changes.
    """

    user_count: int = 10
    post_count: int = 50
    session_count: int = 50
    simulation_days: int = 30
    min_feed_size: int = 10
    max_feed_size: int = 20
    seed: int = 42
    batch_size: int = 10_000

    # Posts may be created before the session window so early sessions
    # still have a usable candidate pool.
    post_history_days: int = 90

    # Candidate pool is larger than the exposed feed.
    candidate_pool_multiplier: float = 3.0

    # Approximate content mix (normalized at runtime).
    content_type_weights: dict[str, float] = field(
        default_factory=lambda: {
            "TEXT": 0.35,
            "IMAGE": 0.35,
            "VIDEO": 0.30,
        }
    )

    # Author posting activity uses a Pareto shape (higher → more even).
    author_activity_shape: float = 1.4

    # Session-count allocation exponent on activity_level.
    activity_session_exponent: float = 1.5

    output_dir: str = "output"
    output_name: str = "simulation_dataset"

    @property
    def parquet_path(self) -> Path:
        return Path(self.output_dir) / f"{self.output_name}.parquet"


# Existing small development configuration (backward compatible scale).
DEV_CONFIG = SimulationConfig(
    user_count=10,
    post_count=50,
    session_count=50,
    simulation_days=30,
    min_feed_size=10,
    max_feed_size=20,
    seed=42,
    batch_size=5_000,
    output_name="dev_dataset",
)

TRAINING_CONFIG = SimulationConfig(
    user_count=1_000,
    post_count=5_000,
    session_count=10_000,

    simulation_days=30,

    min_feed_size=20,
    max_feed_size=50,

    seed=42,

    batch_size=10_000,

    post_history_days=90,

    candidate_pool_multiplier=3.0,

    content_type_weights={
        "TEXT": 0.35,
        "IMAGE": 0.35,
        "VIDEO": 0.30,
    },

    author_activity_shape=1.4,

    activity_session_exponent=1.5,

    output_dir="output",
    output_name="large_test_dataset",
)
