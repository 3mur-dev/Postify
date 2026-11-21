from __future__ import annotations

import copy
import math
import random
from pathlib import Path
from typing import Any

import numpy as np
import pandas as pd
import torch
import torch.nn as nn
from sklearn.metrics import ndcg_score


# ============================================================
# PATHS
# ============================================================

PROJECT_ROOT = Path(__file__).resolve().parent.parent

DATASET_PATH = (
    PROJECT_ROOT
    / "output"
    / "large_test_dataset.parquet"
)

MODEL_PATH = (
    PROJECT_ROOT
    / "models"
    / "postify_ranker_pytorch_v2.pt"
)


# ============================================================
# REPRODUCIBILITY
# ============================================================

RANDOM_STATE = 42


def set_seed(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)

    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)

    # Deterministic behavior is useful for a portfolio experiment.
    # This can reduce CUDA performance slightly.
    torch.backends.cudnn.deterministic = True
    torch.backends.cudnn.benchmark = False


# ============================================================
# DEVICE
# ============================================================

DEVICE = torch.device(
    "cuda"
    if torch.cuda.is_available()
    else "cpu"
)


# ============================================================
# DATA SPLIT
# ============================================================

TRAIN_FRACTION = 0.70
VALIDATION_FRACTION = 0.15


# ============================================================
# TRAINING CONFIGURATION
# ============================================================

BATCH_SIZE = 64

EPOCHS = 50

LEARNING_RATE = 1e-3

WEIGHT_DECAY = 1e-4

PATIENCE = 7

MIN_DELTA = 1e-4

GRADIENT_CLIP_NORM = 1.0


# ============================================================
# MODEL CONFIGURATION
# ============================================================

NUMERIC_FEATURES = [
    "user_activity_level",
    "user_session_length_mean",
    "user_exploration_rate",
    "user_engagement_bias",

    "history_impressions",
    "history_views",
    "history_likes",
    "history_saves",
    "history_comments",
    "history_shares",

    "history_like_rate",
    "history_save_rate",
    "history_comment_rate",
    "history_share_rate",
    "history_engagement_rate",

    "history_avg_dwell_ms",
    "history_avg_completion",

    "duration_ms",
    "topic_count",
    "topic_relevance",

    "post_age_hours",

    "session_hour",
    "session_day_of_week",
]

CATEGORICAL_FEATURES = [
    "content_type",
    "media_type",
]

FEATURE_COLUMNS = (
    NUMERIC_FEATURES
    + CATEGORICAL_FEATURES
)


# ============================================================
# TARGET
# ============================================================

TARGET_COLUMN = "engagement_relevance"

TARGET_LABELS = {
    0: "no_engagement",
    1: "view_or_complete",
    2: "like",
    3: "save_or_comment",
    4: "share",
}


# ============================================================
# RANKING METRICS
# ============================================================

NDCG_K_VALUES = (5, 10, 20)


# ============================================================
# PYTORCH MODEL
# ============================================================

class PostifyRankerV2(nn.Module):
    """
    Neural learning-to-rank model.

    Inputs:
        numeric_features:
            [batch, candidates, num_numeric_features]

        categorical_features:
            [batch, candidates, num_categorical_features]

    Output:
        ranking score:
            [batch, candidates]
    """

    def __init__(
        self,
        num_numeric_features: int,
        categorical_cardinalities: list[int],
        categorical_embedding_dims: list[int],
        hidden_dims: tuple[int, ...] = (128, 64, 32),
        dropout: float = 0.15,
    ) -> None:

        super().__init__()

        if len(categorical_cardinalities) != len(
            categorical_embedding_dims
        ):
            raise ValueError(
                "Categorical cardinalities and embedding "
                "dimensions must have the same length."
            )

        # ----------------------------------------------------
        # Categorical embeddings
        # ----------------------------------------------------

        self.embeddings = nn.ModuleList(
            [
                nn.Embedding(
                    num_embeddings=cardinality,
                    embedding_dim=embedding_dim,
                )
                for cardinality, embedding_dim in zip(
                    categorical_cardinalities,
                    categorical_embedding_dims,
                )
            ]
        )

        embedding_total = sum(
            categorical_embedding_dims
        )

        input_dim = (
            num_numeric_features
            + embedding_total
        )

        # ----------------------------------------------------
        # Ranking MLP
        # ----------------------------------------------------

        layers: list[nn.Module] = []

        current_dim = input_dim

        for hidden_dim in hidden_dims:

            layers.append(
                nn.Linear(
                    current_dim,
                    hidden_dim,
                )
            )

            layers.append(
                nn.LayerNorm(
                    hidden_dim
                )
            )

            layers.append(
                nn.GELU()
            )

            layers.append(
                nn.Dropout(
                    dropout
                )
            )

            current_dim = hidden_dim

        layers.append(
            nn.Linear(
                current_dim,
                1,
            )
        )

        self.network = nn.Sequential(
            *layers
        )

        self._initialize_weights()

    def _initialize_weights(self) -> None:

        for module in self.modules():

            if isinstance(
                module,
                nn.Linear,
            ):
                nn.init.xavier_uniform_(
                    module.weight
                )

                if module.bias is not None:
                    nn.init.zeros_(
                        module.bias
                    )

            elif isinstance(
                module,
                nn.Embedding,
            ):
                nn.init.normal_(
                    module.weight,
                    mean=0.0,
                    std=0.02,
                )

    def forward(
        self,
        numeric_features: torch.Tensor,
        categorical_features: torch.Tensor,
    ) -> torch.Tensor:

        # numeric:
        # [B, N, F]

        # categorical:
        # [B, N, C]

        embedded: list[torch.Tensor] = []

        for index, embedding in enumerate(
            self.embeddings
        ):

            embedded.append(
                embedding(
                    categorical_features[:, :, index]
                )
            )

        # Each embedding:
        # [B, N, embedding_dim]

        combined = [
            numeric_features,
            *embedded,
        ]

        x = torch.cat(
            combined,
            dim=-1,
        )

        scores = self.network(
            x
        ).squeeze(-1)

        # [B, N]

        return scores


# ============================================================
# TARGET CONSTRUCTION
# ============================================================

def build_engagement_target(
    df: pd.DataFrame,
) -> pd.Series:

    required = {
        "viewed",
        "completed",
        "liked",
        "saved",
        "commented",
        "shared",
    }

    missing = (
        required
        - set(df.columns)
    )

    if missing:
        raise ValueError(
            f"Dataset is missing target columns: "
            f"{sorted(missing)}"
        )

    target = pd.Series(
        0,
        index=df.index,
        dtype="int8",
    )

    target.loc[
        df["viewed"].fillna(False).astype(bool)
    ] = 1

    target.loc[
        df["completed"].fillna(False).astype(bool)
    ] = 1

    target.loc[
        df["liked"].fillna(False).astype(bool)
    ] = 2

    target.loc[
        df["saved"].fillna(False).astype(bool)
    ] = 3

    target.loc[
        df["commented"].fillna(False).astype(bool)
    ] = 3

    target.loc[
        df["shared"].fillna(False).astype(bool)
    ] = 4

    return target


# ============================================================
# SESSION-SAFE TEMPORAL SPLIT
# ============================================================

def temporal_session_split(
    df: pd.DataFrame,
) -> tuple[
    pd.DataFrame,
    pd.DataFrame,
    pd.DataFrame,
]:

    required = {
        "session_id",
        "session_started_at",
    }

    missing = required - set(df.columns)

    if missing:
        raise ValueError(
            f"Missing required split columns: "
            f"{sorted(missing)}"
        )

    sessions = (
        df[
            [
                "session_id",
                "session_started_at",
            ]
        ]
        .drop_duplicates("session_id")
        .sort_values(
            "session_started_at"
        )
        .reset_index(drop=True)
    )

    n_sessions = len(sessions)

    train_end = int(
        n_sessions
        * TRAIN_FRACTION
    )

    validation_end = int(
        n_sessions
        * (
            TRAIN_FRACTION
            + VALIDATION_FRACTION
        )
    )

    train_session_ids = set(
        sessions.iloc[
            :train_end
        ]["session_id"]
    )

    validation_session_ids = set(
        sessions.iloc[
            train_end:validation_end
        ]["session_id"]
    )

    test_session_ids = set(
        sessions.iloc[
            validation_end:
        ]["session_id"]
    )

    train = df[
        df["session_id"].isin(
            train_session_ids
        )
    ].copy()

    validation = df[
        df["session_id"].isin(
            validation_session_ids
        )
    ].copy()

    test = df[
        df["session_id"].isin(
            test_session_ids
        )
    ].copy()

    sort_columns = [
        "session_started_at",
        "session_id",
    ]

    train = (
        train
        .sort_values(sort_columns)
        .reset_index(drop=True)
    )

    validation = (
        validation
        .sort_values(sort_columns)
        .reset_index(drop=True)
    )

    test = (
        test
        .sort_values(sort_columns)
        .reset_index(drop=True)
    )

    return (
        train,
        validation,
        test,
    )


# ============================================================
# DATA CONTRACT VALIDATION
# ============================================================

def validate_dataset_contract(
    df: pd.DataFrame,
    name: str,
) -> None:

    print()
    print(
        f"========== {name} CONTRACT CHECK =========="
    )

    required = {
        "session_id",
        "session_started_at",
        "post_created_at",
        "viewed",
        "completed",
        "liked",
        "saved",
        "commented",
        "shared",
    }

    missing = (
        required
        - set(df.columns)
    )

    if missing:
        raise ValueError(
            f"{name} is missing columns: "
            f"{sorted(missing)}"
        )

    session_time = pd.to_datetime(
        df["session_started_at"],
        errors="coerce",
    )

    post_time = pd.to_datetime(
        df["post_created_at"],
        errors="coerce",
    )

    invalid_temporal_rows = (
        session_time < post_time
    ).sum()

    if invalid_temporal_rows:
        raise ValueError(
            f"{name}: "
            f"{invalid_temporal_rows:,} rows occur "
            f"before post creation."
        )

    duplicate_rows = df.duplicated(
        subset=[
            "session_id",
            "post_id",
        ]
    ).sum()

    if duplicate_rows:
        raise ValueError(
            f"{name}: "
            f"{duplicate_rows:,} duplicate "
            f"session/post exposures."
        )

    print("[PASS] Required columns")
    print("[PASS] Post/session temporal ordering")
    print("[PASS] Unique session/post exposure rows")


# ============================================================
# FEATURE PREPROCESSOR
# ============================================================

class FeaturePreprocessor:
    """
    Fits numerical normalization and categorical vocabularies
    on TRAIN ONLY.

    This is important because preprocessing statistics are part
    of the learned pipeline and must never be calculated from
    validation/test data.
    """

    def __init__(
        self,
        numeric_features: list[str],
        categorical_features: list[str],
    ) -> None:

        self.numeric_features = list(
            numeric_features
        )

        self.categorical_features = list(
            categorical_features
        )

        self.numeric_mean: dict[str, float] = {}
        self.numeric_std: dict[str, float] = {}

        self.category_mappings: dict[
            str,
            dict[str, int],
        ] = {}

    def fit(
        self,
        df: pd.DataFrame,
    ) -> None:

        # ----------------------------------------------------
        # Numerical statistics
        # ----------------------------------------------------

        for column in self.numeric_features:

            values = pd.to_numeric(
                df[column],
                errors="coerce",
            )

            values = values.replace(
                [np.inf, -np.inf],
                np.nan,
            )

            mean = float(
                values.mean()
            )

            std = float(
                values.std()
            )

            if not math.isfinite(mean):
                mean = 0.0

            if (
                not math.isfinite(std)
                or std < 1e-8
            ):
                std = 1.0

            self.numeric_mean[column] = mean
            self.numeric_std[column] = std

        # ----------------------------------------------------
        # Categorical vocabularies
        # ----------------------------------------------------

        for column in self.categorical_features:

            values = (
                df[column]
                .fillna("UNKNOWN")
                .astype(str)
            )

            unique_values = sorted(
                values.unique()
            )

            # 0 is reserved for UNKNOWN.
            mapping = {
                "UNKNOWN": 0
            }

            next_index = 1

            for value in unique_values:

                if value == "UNKNOWN":
                    continue

                mapping[value] = next_index
                next_index += 1

            self.category_mappings[column] = mapping

    def transform(
        self,
        df: pd.DataFrame,
    ) -> tuple[
        np.ndarray,
        np.ndarray,
    ]:

        # ----------------------------------------------------
        # Numerical matrix
        # ----------------------------------------------------

        numeric_arrays: list[np.ndarray] = []

        for column in self.numeric_features:

            values = pd.to_numeric(
                df[column],
                errors="coerce",
            )

            values = values.replace(
                [np.inf, -np.inf],
                np.nan,
            )

            values = values.fillna(
                self.numeric_mean[column]
            )

            normalized = (
                values.to_numpy(
                    dtype=np.float32
                )
                - self.numeric_mean[column]
            ) / self.numeric_std[column]

            numeric_arrays.append(
                normalized
            )

        numeric_matrix = np.column_stack(
            numeric_arrays
        ).astype(
            np.float32
        )

        # ----------------------------------------------------
        # Categorical matrix
        # ----------------------------------------------------

        categorical_arrays: list[np.ndarray] = []

        for column in self.categorical_features:

            mapping = (
                self.category_mappings[column]
            )

            values = (
                df[column]
                .fillna("UNKNOWN")
                .astype(str)
            )

            encoded = (
                values
                .map(mapping)
                .fillna(0)
                .to_numpy(
                    dtype=np.int64
                )
            )

            categorical_arrays.append(
                encoded
            )

        categorical_matrix = np.column_stack(
            categorical_arrays
        ).astype(
            np.int64
        )

        return (
            numeric_matrix,
            categorical_matrix,
        )

    def cardinalities(self) -> list[int]:

        return [
            len(
                self.category_mappings[column]
            )
            for column in self.categorical_features
        ]

    def export(self) -> dict[str, Any]:

        return {
            "numeric_features": list(
                self.numeric_features
            ),

            "categorical_features": list(
                self.categorical_features
            ),

            "numeric_mean": dict(
                self.numeric_mean
            ),

            "numeric_std": dict(
                self.numeric_std
            ),

            "category_mappings": copy.deepcopy(
                self.category_mappings
            ),
        }


# ============================================================
# RANKING GROUP REPRESENTATION
# ============================================================

class RankingGroup:
    """
    One session = one ranking group.
    """

    def __init__(
        self,
        numeric: np.ndarray,
        categorical: np.ndarray,
        target: np.ndarray,
    ) -> None:

        if len(numeric) != len(target):
            raise ValueError(
                "Numeric features and targets "
                "must have identical row counts."
            )

        if len(categorical) != len(target):
            raise ValueError(
                "Categorical features and targets "
                "must have identical row counts."
            )

        self.numeric = torch.from_numpy(
            numeric
        )

        self.categorical = torch.from_numpy(
            categorical
        )

        self.target = torch.from_numpy(
            target.astype(
                np.int64
            )
        )

    @property
    def size(self) -> int:
        return len(self.target)


# ============================================================
# BUILD RANKING GROUPS
# ============================================================

def build_ranking_groups(
    features: pd.DataFrame,
    targets: np.ndarray,
    session_ids: pd.Series,
    preprocessor: FeaturePreprocessor,
) -> list[RankingGroup]:

    numeric_matrix, categorical_matrix = (
        preprocessor.transform(features)
    )

    if len(targets) != len(features):
        raise ValueError(
            "Target length does not match feature rows."
        )

    if len(session_ids) != len(features):
        raise ValueError(
            "Session ID length does not match feature rows."
        )

    groups: list[RankingGroup] = []

    session_positions = (
        session_ids
        .groupby(
            session_ids,
            sort=False,
        )
        .indices
    )

    for positions in session_positions.values():

        positions_array = np.asarray(
            positions,
            dtype=np.int64,
        )

        groups.append(
            RankingGroup(
                numeric=numeric_matrix[
                    positions_array
                ],

                categorical=categorical_matrix[
                    positions_array
                ],

                target=targets[
                    positions_array
                ],
            )
        )

    if not groups:
        raise ValueError(
            "No ranking groups were created."
        )

    return groups


# ============================================================
# BATCH COLLATION
# ============================================================

def collate_groups(
    groups: list[RankingGroup],
) -> dict[str, torch.Tensor]:

    batch_size = len(groups)

    max_candidates = max(
        group.size
        for group in groups
    )

    num_numeric = groups[0].numeric.shape[1]
    num_categorical = groups[0].categorical.shape[1]

    numeric = torch.zeros(
        (
            batch_size,
            max_candidates,
            num_numeric,
        ),
        dtype=torch.float32,
    )

    categorical = torch.zeros(
        (
            batch_size,
            max_candidates,
            num_categorical,
        ),
        dtype=torch.long,
    )

    target = torch.zeros(
        (
            batch_size,
            max_candidates,
        ),
        dtype=torch.long,
    )

    mask = torch.zeros(
        (
            batch_size,
            max_candidates,
        ),
        dtype=torch.bool,
    )

    for batch_index, group in enumerate(
        groups
    ):

        length = group.size

        numeric[
            batch_index,
            :length,
        ] = group.numeric

        categorical[
            batch_index,
            :length,
        ] = group.categorical

        target[
            batch_index,
            :length,
        ] = group.target

        mask[
            batch_index,
            :length,
        ] = True

    return {
        "numeric": numeric,
        "categorical": categorical,
        "target": target,
        "mask": mask,
    }


# ============================================================
# ITERATOR
# ============================================================

def iter_group_batches(
    groups: list[RankingGroup],
    batch_size: int,
    shuffle: bool,
) -> Any:

    indices = np.arange(
        len(groups)
    )

    if shuffle:
        np.random.shuffle(
            indices
        )

    for start in range(
        0,
        len(indices),
        batch_size,
    ):

        batch_indices = indices[
            start:
            start + batch_size
        ]

        yield [
            groups[index]
            for index in batch_indices
        ]


# ============================================================
# LISTWISE LOSS
# ============================================================

def listnet_ranking_loss(
    scores: torch.Tensor,
    relevance: torch.Tensor,
    mask: torch.Tensor,
) -> torch.Tensor:

    """
    ListNet-style listwise ranking loss.

    Relevance grades are converted into gain values:

        0 -> 0
        1 -> 1
        2 -> 3
        3 -> 7
        4 -> 15

    Positive gain is then normalized into a target
    probability distribution across candidates.

    The model learns to place more probability mass on
    higher-value candidates.

    Sessions containing no positive engagement are skipped
    because there is no meaningful ranking preference to learn.
    """

    device = scores.device

    gains_lookup = torch.tensor(
        [
            0.0,
            1.0,
            3.0,
            7.0,
            15.0,
        ],
        dtype=torch.float32,
        device=device,
    )

    gains = gains_lookup[
        relevance
    ]

    # Mask padding.
    gains = gains.masked_fill(
        ~mask,
        0.0,
    )

    gain_sum = gains.sum(
        dim=1,
        keepdim=True,
    )

    valid_groups = (
        gain_sum.squeeze(1) > 0
    )

    if not torch.any(valid_groups):
        return scores.sum() * 0.0

    target_distribution = (
        gains[valid_groups]
        / gain_sum[valid_groups]
    )

    valid_scores = scores[
        valid_groups
    ]

    valid_mask = mask[
        valid_groups
    ]

    # Mask padding before softmax.
    valid_scores = valid_scores.masked_fill(
        ~valid_mask,
        -1e9,
    )

    log_predictions = torch.log_softmax(
        valid_scores,
        dim=1,
    )

    loss = -(
        target_distribution
        * log_predictions
    ).sum(
        dim=1
    )

    return loss.mean()


# ============================================================
# FORWARD BATCH
# ============================================================

def move_batch_to_device(
    batch: dict[str, torch.Tensor],
    device: torch.device,
) -> dict[str, torch.Tensor]:

    return {
        key: value.to(
            device,
            non_blocking=True,
        )
        for key, value in batch.items()
    }


def prepare_features(
    df: pd.DataFrame,
) -> pd.DataFrame:

    required = (
        set(NUMERIC_FEATURES)
        - {
            "post_age_hours",
            "session_hour",
            "session_day_of_week",
        }
    ) | set(CATEGORICAL_FEATURES)

    missing = required - set(df.columns)

    if missing:
        raise ValueError(
            f"Missing required raw features: "
            f"{sorted(missing)}"
        )

    features = df[
        [
            column
            for column in FEATURE_COLUMNS
            if column not in {
                "post_age_hours",
                "session_hour",
                "session_day_of_week",
            }
        ]
    ].copy()

    # --------------------------------------------------------
    # Derived temporal features
    # --------------------------------------------------------

    post_created = pd.to_datetime(
        df["post_created_at"],
        errors="coerce",
    )

    session_started = pd.to_datetime(
        df["session_started_at"],
        errors="coerce",
    )

    if post_created.isna().any():
        raise ValueError(
            "post_created_at contains invalid timestamps."
        )

    if session_started.isna().any():
        raise ValueError(
            "session_started_at contains invalid timestamps."
        )

    age_hours = (
        session_started - post_created
    ).dt.total_seconds() / 3600.0

    if (age_hours < 0).any():
        raise ValueError(
            "post_age_hours contains negative values."
        )

    features["post_age_hours"] = (
        age_hours.astype("float32")
    )

    features["session_hour"] = (
        session_started.dt.hour.astype("int8")
    )

    features["session_day_of_week"] = (
        session_started.dt.dayofweek.astype("int8")
    )

    # --------------------------------------------------------
    # Numeric normalization happens later.
    # Just make sure values are numeric.
    # --------------------------------------------------------

    for column in NUMERIC_FEATURES:
        features[column] = pd.to_numeric(
            features[column],
            errors="coerce",
        )

    features[NUMERIC_FEATURES] = (
        features[NUMERIC_FEATURES]
        .replace(
            [np.inf, -np.inf],
            np.nan,
        )
    )

    # --------------------------------------------------------
    # Categorical values
    # --------------------------------------------------------

    for column in CATEGORICAL_FEATURES:
        features[column] = (
            features[column]
            .fillna("UNKNOWN")
            .astype(str)
        )

    return features[
        FEATURE_COLUMNS
    ]

# ============================================================
# TRAIN ONE EPOCH
# ============================================================

def train_epoch(
    model: PostifyRankerV2,
    groups: list[RankingGroup],
    optimizer: torch.optim.Optimizer,
    device: torch.device,
) -> float:

    model.train()

    total_loss = 0.0
    batch_count = 0

    for group_batch in iter_group_batches(
        groups,
        BATCH_SIZE,
        shuffle=True,
    ):

        batch = collate_groups(
            group_batch
        )

        batch = move_batch_to_device(
            batch,
            device,
        )

        optimizer.zero_grad(
            set_to_none=True
        )

        scores = model(
            batch["numeric"],
            batch["categorical"],
        )

        loss = listnet_ranking_loss(
            scores=scores,
            relevance=batch["target"],
            mask=batch["mask"],
        )

        loss.backward()

        torch.nn.utils.clip_grad_norm_(
            model.parameters(),
            max_norm=GRADIENT_CLIP_NORM,
        )

        optimizer.step()

        total_loss += float(
            loss.item()
        )

        batch_count += 1

    if batch_count == 0:
        return 0.0

    return total_loss / batch_count


# ============================================================
# PREDICT GROUPS
# ============================================================

def predict_groups(
    model: PostifyRankerV2,
    groups: list[RankingGroup],
    device: torch.device,
) -> list[np.ndarray]:

    model.eval()

    predictions: list[np.ndarray] = []

    with torch.no_grad():

        for group_batch in iter_group_batches(
            groups,
            BATCH_SIZE,
            shuffle=False,
        ):

            batch = collate_groups(
                group_batch
            )

            batch = move_batch_to_device(
                batch,
                device,
            )

            scores = model(
                batch["numeric"],
                batch["categorical"],
            )

            scores = (
                scores
                .detach()
                .cpu()
                .numpy()
            )

            mask = (
                batch["mask"]
                .detach()
                .cpu()
                .numpy()
            )

            for row_index in range(
                len(group_batch)
            ):

                length = int(
                    mask[
                        row_index
                    ].sum()
                )

                predictions.append(
                    scores[
                        row_index,
                        :length,
                    ]
                )

    return predictions


# ============================================================
# RANKING METRICS
# ============================================================

def calculate_metrics(
    groups: list[RankingGroup],
    predictions: list[np.ndarray],
) -> dict[str, float]:

    metrics: dict[str, float] = {}

    for k in NDCG_K_VALUES:

        ndcg_values: list[float] = []

        recall_values: list[float] = []

        for group, scores in zip(
            groups,
            predictions,
        ):

            relevance = (
                group.target
                .numpy()
            )

            if not np.any(
                relevance > 0
            ):
                continue

            ndcg_values.append(
                float(
                    ndcg_score(
                        [relevance],
                        [scores],
                        k=k,
                    )
                )
            )

            top_k = min(
                k,
                len(relevance),
            )

            ranked_indices = np.argsort(
                scores
            )[::-1][
                :top_k
            ]

            positive_count = int(
                np.sum(
                    relevance > 0
                )
            )

            hits = int(
                np.sum(
                    relevance[
                        ranked_indices
                    ] > 0
                )
            )

            recall_values.append(
                hits
                / positive_count
            )

        metrics[
            f"ndcg@{k}"
        ] = (
            float(
                np.mean(
                    ndcg_values
                )
            )
            if ndcg_values
            else 0.0
        )

        metrics[
            f"recall@{k}"
        ] = (
            float(
                np.mean(
                    recall_values
                )
            )
            if recall_values
            else 0.0
        )

    return metrics


def print_metrics(
    name: str,
    metrics: dict[str, float],
) -> None:

    print()
    print(
        f"========== {name} RANKING =========="
    )

    for k in NDCG_K_VALUES:

        print(
            f"NDCG@{k:<2}:   "
            f"{metrics[f'ndcg@{k}']:.4f}"
        )

        print(
            f"Recall@{k:<2}:  "
            f"{metrics[f'recall@{k}']:.4f}"
        )


# ============================================================
# FEATURE IMPORTANCE / PARAMETER STATISTICS
# ============================================================

def print_model_summary(
    model: PostifyRankerV2,
) -> None:

    parameter_count = sum(
        parameter.numel()
        for parameter in model.parameters()
    )

    trainable_count = sum(
        parameter.numel()
        for parameter in model.parameters()
        if parameter.requires_grad
    )

    print()
    print(
        "========== MODEL =========="
    )

    print(
        f"Total parameters:     "
        f"{parameter_count:,}"
    )

    print(
        f"Trainable parameters: "
        f"{trainable_count:,}"
    )

    print(
        "Architecture:"
    )

    print(
        model
    )


# ============================================================
# SAVE MODEL ARTIFACT
# ============================================================

def save_model(
    path: Path,
    model: PostifyRankerV2,
    preprocessor: FeaturePreprocessor,
    best_iteration: int,
    validation_metrics: dict[str, float],
    test_metrics: dict[str, float],
) -> None:

    path.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    artifact = {
        "model_version": "pytorch_v2",

        "framework": "pytorch",

        "architecture": {
            "hidden_dims": (
                128,
                64,
                32,
            ),

            "dropout": 0.15,

            "categorical_embedding_dims": [
                8,
                8,
            ],
        },

        "objective": (
            "listnet_listwise_ranking"
        ),

        "target_column": (
            TARGET_COLUMN
        ),

        "target_labels": dict(
            TARGET_LABELS
        ),

        "feature_columns": list(
            FEATURE_COLUMNS
        ),

        "numeric_features": list(
            NUMERIC_FEATURES
        ),

        "categorical_features": list(
            CATEGORICAL_FEATURES
        ),

        "preprocessor": (
            preprocessor.export()
        ),

        "categorical_cardinalities": (
            preprocessor.cardinalities()
        ),

        "categorical_embedding_dims": [
            8,
            8,
        ],

        "best_iteration": (
            best_iteration
        ),

        "validation_metrics": (
            validation_metrics
        ),

        "test_metrics": (
            test_metrics
        ),

        "model_state_dict": (
            model.state_dict()
        ),
    }

    torch.save(
        artifact,
        path,
    )


# ============================================================
# MAIN
# ============================================================

def main() -> None:

    set_seed(
        RANDOM_STATE
    )

    print(
        "=" * 60
    )

    print(
        "POSTIFY PYTORCH RANKER V2"
    )

    print(
        "=" * 60
    )

    print()
    print(
        f"Device: {DEVICE}"
    )

    if torch.cuda.is_available():
        print(
            f"GPU: "
            f"{torch.cuda.get_device_name(0)}"
        )

    # --------------------------------------------------------
    # LOAD DATASET
    # --------------------------------------------------------

    print()
    print(
        "Loading dataset..."
    )

    df = pd.read_parquet(
        DATASET_PATH
    )

    print(
        f"Rows:     {len(df):,}"
    )

    print(
        f"Sessions: "
        f"{df['session_id'].nunique():,}"
    )

    # --------------------------------------------------------
    # FULL DATASET CONTRACT
    # --------------------------------------------------------

    validate_dataset_contract(
        df,
        "FULL DATASET",
    )

    # --------------------------------------------------------
    # TARGET
    # --------------------------------------------------------

    df[
        TARGET_COLUMN
    ] = build_engagement_target(
        df
    )

    print()
    print(
        "========== TARGET DISTRIBUTION =========="
    )

    counts = (
        df[TARGET_COLUMN]
        .value_counts()
        .sort_index()
    )

    for label, count in counts.items():

        percentage = (
            count
            / len(df)
            * 100
        )

        print(
            f"{label} "
            f"({TARGET_LABELS[int(label)]:<18}) "
            f"{count:>8,} "
            f"({percentage:6.2f}%)"
        )

    # --------------------------------------------------------
    # TEMPORAL SESSION SPLIT
    # --------------------------------------------------------

    (
        train_df,
        validation_df,
        test_df,
    ) = temporal_session_split(
        df
    )

    print()
    print(
        "========== TEMPORAL SPLIT =========="
    )

    print(
        f"Train:      "
        f"{len(train_df):,} rows"
    )

    print(
        f"Validation: "
        f"{len(validation_df):,} rows"
    )

    print(
        f"Test:       "
        f"{len(test_df):,} rows"
    )

    print(
        f"Train sessions:      "
        f"{train_df['session_id'].nunique():,}"
    )

    print(
        f"Validation sessions: "
        f"{validation_df['session_id'].nunique():,}"
    )

    print(
        f"Test sessions:       "
        f"{test_df['session_id'].nunique():,}"
    )

    # --------------------------------------------------------
    # PREPROCESSOR
    # --------------------------------------------------------
    #
    # IMPORTANT:
    # FIT ONLY ON TRAIN.
    # --------------------------------------------------------

    preprocessor = FeaturePreprocessor(
        numeric_features=NUMERIC_FEATURES,
        categorical_features=CATEGORICAL_FEATURES,
    )

    train_features = prepare_features(
        train_df
    )

    validation_features = prepare_features(
        validation_df
    )

    test_features = prepare_features(
        test_df
    )

    preprocessor.fit(
        train_features
    )

    # --------------------------------------------------------
    # BUILD GROUPS
    # --------------------------------------------------------

    print()
    print(
        "========== BUILDING RANKING GROUPS =========="
    )

    train_groups = build_ranking_groups(
        train_features,
        train_df[TARGET_COLUMN].to_numpy(),
        train_df["session_id"],
        preprocessor,
    )

    validation_groups = build_ranking_groups(
        validation_features,
        validation_df[TARGET_COLUMN].to_numpy(),
        validation_df["session_id"],
        preprocessor,
    )

    test_groups = build_ranking_groups(
        test_features,
        test_df[TARGET_COLUMN].to_numpy(),
        test_df["session_id"],
        preprocessor,
    )

    print(
        f"Train groups:      "
        f"{len(train_groups):,}"
    )

    print(
        f"Validation groups: "
        f"{len(validation_groups):,}"
    )

    print(
        f"Test groups:       "
        f"{len(test_groups):,}"
    )

    print(
        f"Average train candidates/session: "
        f"{np.mean([g.size for g in train_groups]):.2f}"
    )

    print(
        f"Median train candidates/session:  "
        f"{np.median([g.size for g in train_groups]):.0f}"
    )

    # --------------------------------------------------------
    # MODEL
    # --------------------------------------------------------

    categorical_cardinalities = (
        preprocessor.cardinalities()
    )

    model = PostifyRankerV2(
        num_numeric_features=len(
            NUMERIC_FEATURES
        ),

        categorical_cardinalities=(
            categorical_cardinalities
        ),

        categorical_embedding_dims=[
            8,
            8,
        ],

        hidden_dims=(
            128,
            64,
            32,
        ),

        dropout=0.15,
    ).to(
        DEVICE
    )

    print_model_summary(
        model
    )

    # --------------------------------------------------------
    # OPTIMIZER
    # --------------------------------------------------------

    optimizer = torch.optim.AdamW(
        model.parameters(),
        lr=LEARNING_RATE,
        weight_decay=WEIGHT_DECAY,
    )

    # --------------------------------------------------------
    # TRAINING
    # --------------------------------------------------------

    print()
    print(
        "========== TRAINING =========="
    )

    best_validation_ndcg10 = -np.inf

    best_state_dict = None

    best_epoch = 0

    epochs_without_improvement = 0

    history: list[dict[str, float]] = []

    for epoch in range(
        1,
        EPOCHS + 1,
    ):

        train_loss = train_epoch(
            model=model,
            groups=train_groups,
            optimizer=optimizer,
            device=DEVICE,
        )

        validation_predictions = (
            predict_groups(
                model,
                validation_groups,
                DEVICE,
            )
        )

        validation_metrics = (
            calculate_metrics(
                validation_groups,
                validation_predictions,
            )
        )

        validation_ndcg10 = (
            validation_metrics[
                "ndcg@10"
            ]
        )

        history.append(
            {
                "epoch": float(epoch),

                "train_loss": float(
                    train_loss
                ),

                "validation_ndcg@5": (
                    validation_metrics[
                        "ndcg@5"
                    ]
                ),

                "validation_ndcg@10": (
                    validation_metrics[
                        "ndcg@10"
                    ]
                ),

                "validation_ndcg@20": (
                    validation_metrics[
                        "ndcg@20"
                    ]
                ),
            }
        )

        print(
            f"Epoch {epoch:02d} | "
            f"loss={train_loss:.5f} | "
            f"NDCG@5="
            f"{validation_metrics['ndcg@5']:.4f} | "
            f"NDCG@10="
            f"{validation_metrics['ndcg@10']:.4f} | "
            f"NDCG@20="
            f"{validation_metrics['ndcg@20']:.4f}"
        )

        # ----------------------------------------------------
        # Early stopping
        # ----------------------------------------------------

        if (
            validation_ndcg10
            >
            best_validation_ndcg10
            + MIN_DELTA
        ):

            best_validation_ndcg10 = (
                validation_ndcg10
            )

            best_state_dict = copy.deepcopy(
                model.state_dict()
            )

            best_epoch = epoch

            epochs_without_improvement = 0

            print(
                "  [BEST] validation NDCG@10 improved"
            )

        else:

            epochs_without_improvement += 1

            if (
                epochs_without_improvement
                >= PATIENCE
            ):

                print(
                    f"  [EARLY STOP] "
                    f"no improvement for "
                    f"{PATIENCE} epochs"
                )

                break

    if best_state_dict is None:
        raise RuntimeError(
            "Training never produced a valid "
            "validation checkpoint."
        )

    # --------------------------------------------------------
    # RESTORE BEST MODEL
    # --------------------------------------------------------

    model.load_state_dict(
        best_state_dict
    )

    # --------------------------------------------------------
    # FINAL EVALUATION
    # --------------------------------------------------------

    print()
    print(
        "========== FINAL EVALUATION =========="
    )

    validation_predictions = predict_groups(
        model,
        validation_groups,
        DEVICE,
    )

    test_predictions = predict_groups(
        model,
        test_groups,
        DEVICE,
    )

    validation_metrics = calculate_metrics(
        validation_groups,
        validation_predictions,
    )

    test_metrics = calculate_metrics(
        test_groups,
        test_predictions,
    )

    print_metrics(
        "VALIDATION",
        validation_metrics,
    )

    print_metrics(
        "TEST",
        test_metrics,
    )

    # --------------------------------------------------------
    # COMPARISON TO LIGHTGBM BENCHMARK
    # --------------------------------------------------------

    LIGHTGBM_NDCG10 = 0.6214
    LIGHTGBM_NDCG20 = 0.6849

    pytorch_ndcg10 = test_metrics[
        "ndcg@10"
    ]

    pytorch_ndcg20 = test_metrics[
        "ndcg@20"
    ]

    print()
    print(
        "========== BENCHMARK =========="
    )

    print(
        f"LightGBM V2 Test NDCG@10: "
        f"{LIGHTGBM_NDCG10:.4f}"
    )

    print(
        f"PyTorch V2 Test NDCG@10:   "
        f"{pytorch_ndcg10:.4f}"
    )

    print(
        f"NDCG@10 difference:         "
        f"{pytorch_ndcg10 - LIGHTGBM_NDCG10:+.4f}"
    )

    print()
    print(
        f"LightGBM V2 Test NDCG@20: "
        f"{LIGHTGBM_NDCG20:.4f}"
    )

    print(
        f"PyTorch V2 Test NDCG@20:   "
        f"{pytorch_ndcg20:.4f}"
    )

    print(
        f"NDCG@20 difference:         "
        f"{pytorch_ndcg20 - LIGHTGBM_NDCG20:+.4f}"
    )

    # --------------------------------------------------------
    # SAVE ARTIFACT
    # --------------------------------------------------------

    save_model(
        path=MODEL_PATH,
        model=model,
        preprocessor=preprocessor,
        best_iteration=best_epoch,
        validation_metrics=validation_metrics,
        test_metrics=test_metrics,
    )

    print()
    print(
        f"Model saved to:"
    )

    print(
        MODEL_PATH
    )

    print()
    print(
        "=" * 60
    )

    print(
        "POSTIFY PYTORCH RANKER V2 COMPLETE"
    )

    print(
        "=" * 60
    )


if __name__ == "__main__":
    main()