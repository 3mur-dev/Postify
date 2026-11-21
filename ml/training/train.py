from __future__ import annotations

from pathlib import Path
from typing import Iterable

import joblib
import lightgbm as lgb
import numpy as np
import pandas as pd
from sklearn.metrics import ndcg_score


PROJECT_ROOT = Path(__file__).resolve().parent.parent

DATASET_PATH = (
    PROJECT_ROOT
    / "output"
    / "large_test_dataset.parquet"
)

MODEL_PATH = (
    PROJECT_ROOT
    / "models"
    / "postify_ranker_v2.pkl"
)


# ============================================================
# CONFIGURATION
# ============================================================

RANDOM_STATE = 42

TRAIN_FRACTION = 0.70
VALIDATION_FRACTION = 0.15

NDCG_K_VALUES = (5, 10, 20)

EARLY_STOPPING_ROUNDS = 100


# ============================================================
# FEATURE CONTRACT
# ============================================================
#
# These are the features available BEFORE the ranking decision.
#
# Deliberately excluded:
#
#   user_id
#   post_id
#   author_id
#
# Raw IDs are identifiers, not meaningful numerical features.
#
# Also excluded:
#
#   impressed
#   viewed
#
# Those are outcomes of the exposure itself. The ranking model
# must make its decision before the user views the post.
#
# session_duration_seconds is also excluded because we do not
# want to risk using the eventual duration of a session.
#
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
#
# We are no longer predicting only "liked".
#
# The feed should prefer stronger forms of engagement.
#
# Relevance grades:
#
#   0 = no meaningful engagement
#   1 = viewed/completed
#   2 = liked
#   3 = saved/commented
#   4 = shared
#
# We use the strongest interaction achieved for the exposure
# rather than adding points together. This prevents:
#
#   view + like + save
#
# from becoming an artificially huge target.
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
# SESSION-BOUNDARY TEMPORAL SPLIT
# ============================================================

def temporal_session_split(
    df: pd.DataFrame,
) -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:

    required = {
        "session_id",
        "session_started_at",
    }

    missing = required - set(df.columns)

    if missing:
        raise ValueError(
            f"Dataset is missing required split columns: "
            f"{sorted(missing)}"
        )

    session_table = (
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

    n_sessions = len(session_table)

    train_end = int(
        n_sessions * TRAIN_FRACTION
    )

    validation_end = int(
        n_sessions
        * (TRAIN_FRACTION + VALIDATION_FRACTION)
    )

    train_sessions = set(
        session_table.iloc[:train_end]["session_id"]
    )

    validation_sessions = set(
        session_table.iloc[
            train_end:validation_end
        ]["session_id"]
    )

    test_sessions = set(
        session_table.iloc[
            validation_end:
        ]["session_id"]
    )

    train = df[
        df["session_id"].isin(train_sessions)
    ].copy()

    validation = df[
        df["session_id"].isin(validation_sessions)
    ].copy()

    test = df[
        df["session_id"].isin(test_sessions)
    ].copy()

    # Keep each split ordered chronologically.
    train = train.sort_values(
        [
            "session_started_at",
            "session_id",
        ]
    ).reset_index(drop=True)

    validation = validation.sort_values(
        [
            "session_started_at",
            "session_id",
        ]
    ).reset_index(drop=True)

    test = test.sort_values(
        [
            "session_started_at",
            "session_id",
        ]
    ).reset_index(drop=True)

    return train, validation, test


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

    missing = required - set(df.columns)

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

    # Basic positive consumption.
    target.loc[
        df["viewed"].astype(bool)
    ] = 1

    target.loc[
        df["completed"].fillna(False).astype(bool)
    ] = 1

    # Explicit positive feedback.
    target.loc[
        df["liked"].astype(bool)
    ] = 2

    # Stronger intentional engagement.
    target.loc[
        df["saved"].astype(bool)
    ] = 3

    target.loc[
        df["commented"].astype(bool)
    ] = 3

    # Strongest explicit signal.
    target.loc[
        df["shared"].astype(bool)
    ] = 4

    return target


# ============================================================
# FEATURE ENGINEERING
# ============================================================

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
            f"Missing required features: "
            f"{sorted(missing)}"
        )

    # Start with features that already exist in the dataset.
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
    # Derived post age
    # --------------------------------------------------------

    if "post_created_at" not in df.columns:
        raise ValueError(
            "post_created_at is required "
            "to calculate post_age_hours."
        )

    if "session_started_at" not in df.columns:
        raise ValueError(
            "session_started_at is required "
            "to calculate post_age_hours."
        )

    post_created = pd.to_datetime(
        df["post_created_at"],
        errors="coerce",
    )

    session_started = pd.to_datetime(
        df["session_started_at"],
        errors="coerce",
    )

    age_hours = (
        session_started - post_created
    ).dt.total_seconds() / 3600.0

    if age_hours.isna().any():
        raise ValueError(
            "Unable to calculate post_age_hours "
            "for one or more rows."
        )

    if (age_hours < 0).any():
        raise ValueError(
            "post_age_hours contains negative values. "
            "This indicates invalid temporal data."
        )

    features["post_age_hours"] = age_hours.astype(
        "float32"
    )

    # --------------------------------------------------------
    # Session time features
    # --------------------------------------------------------

    features["session_hour"] = (
        session_started.dt.hour
        .astype("int8")
    )

    features["session_day_of_week"] = (
        session_started.dt.dayofweek
        .astype("int8")
    )

    # --------------------------------------------------------
    # Numerical features
    # --------------------------------------------------------

    numeric_columns = [
        column
        for column in NUMERIC_FEATURES
        if column not in {
            "post_age_hours",
            "session_hour",
            "session_day_of_week",
        }
    ]

    for column in numeric_columns:
        features[column] = pd.to_numeric(
            features[column],
            errors="coerce",
        )

    features[numeric_columns] = (
        features[numeric_columns]
        .replace(
            [np.inf, -np.inf],
            np.nan,
        )
    )

    # --------------------------------------------------------
    # Categorical features
    # --------------------------------------------------------

    for column in CATEGORICAL_FEATURES:
        features[column] = (
            features[column]
            .fillna("UNKNOWN")
            .astype("category")
        )

    # --------------------------------------------------------
    # Final ordering
    # --------------------------------------------------------

    features = features[
        FEATURE_COLUMNS
    ]

    return features


# ============================================================
# GROUP INFORMATION
# ============================================================

def get_session_groups(
    df: pd.DataFrame,
) -> list[int]:

    groups = (
        df.groupby(
            "session_id",
            sort=False,
        )
        .size()
        .tolist()
    )

    if not groups:
        raise ValueError(
            "No ranking groups were found."
        )

    if sum(groups) != len(df):
        raise RuntimeError(
            "Ranking group sizes do not match "
            "the number of rows."
        )

    return groups


# ============================================================
# DATASET VALIDATION
# ============================================================

def validate_dataset_contract(
    df: pd.DataFrame,
    name: str,
) -> None:

    print()
    print(f"========== {name} CONTRACT CHECK ==========")

    required_columns = {
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
        required_columns
        - set(df.columns)
    )

    if missing:
        raise ValueError(
            f"{name} missing required columns: "
            f"{sorted(missing)}"
        )

    # No post may appear before it was created.
    post_created = pd.to_datetime(
        df["post_created_at"]
    )

    session_started = pd.to_datetime(
        df["session_started_at"]
    )

    invalid_time = (
        session_started < post_created
    ).sum()

    if invalid_time:
        raise ValueError(
            f"{name}: {invalid_time:,} rows "
            f"occur before post creation."
        )

    duplicate_keys = df.duplicated(
        subset=[
            "session_id",
            "post_id",
        ]
    ).sum()

    if duplicate_keys:
        raise ValueError(
            f"{name}: {duplicate_keys:,} duplicate "
            f"(session_id, post_id) rows."
        )

    print("[PASS] Required columns")
    print("[PASS] Post/session temporal ordering")
    print("[PASS] Unique session/post exposure rows")


# ============================================================
# RANKING METRICS
# ============================================================

def iter_groups(
    values: Iterable,
    groups: list[int],
):
    start = 0

    for group_size in groups:
        end = start + group_size

        yield values[start:end]

        start = end


def grouped_ndcg(
    y_true: np.ndarray,
    scores: np.ndarray,
    groups: list[int],
    k: int,
) -> float:

    values: list[float] = []

    for group_y, group_scores in zip(
        iter_groups(y_true, groups),
        iter_groups(scores, groups),
    ):

        # NDCG is meaningless for a group with no positive
        # relevance at all, so exclude it from the average.
        if not np.any(group_y > 0):
            continue

        values.append(
            float(
                ndcg_score(
                    [group_y],
                    [group_scores],
                    k=k,
                )
            )
        )

    if not values:
        return 0.0

    return float(np.mean(values))


def grouped_recall_at_k(
    y_true: np.ndarray,
    scores: np.ndarray,
    groups: list[int],
    k: int,
) -> float:

    recalls: list[float] = []

    for group_y, group_scores in zip(
        iter_groups(y_true, groups),
        iter_groups(scores, groups),
    ):

        positive_count = int(
            np.sum(group_y > 0)
        )

        if positive_count == 0:
            continue

        k_actual = min(
            k,
            len(group_y),
        )

        top_indices = np.argsort(
            group_scores
        )[::-1][:k_actual]

        hits = int(
            np.sum(
                group_y[top_indices] > 0
            )
        )

        recalls.append(
            hits / positive_count
        )

    if not recalls:
        return 0.0

    return float(np.mean(recalls))


def evaluate_ranker(
    model: lgb.LGBMRanker,
    X: pd.DataFrame,
    y: pd.Series,
    groups: list[int],
    split_name: str,
) -> None:

    scores = model.predict(X)

    print()
    print(f"========== {split_name} RANKING ==========")

    print(
        f"Sessions: {len(groups):,}"
    )

    print(
        f"Rows:     {len(X):,}"
    )

    for k in NDCG_K_VALUES:

        ndcg = grouped_ndcg(
            y.to_numpy(),
            scores,
            groups,
            k,
        )

        recall = grouped_recall_at_k(
            y.to_numpy(),
            scores,
            groups,
            k,
        )

        print(
            f"NDCG@{k:<2}:   {ndcg:.4f}"
        )

        print(
            f"Recall@{k:<2}:  {recall:.4f}"
        )


# ============================================================
# FEATURE VALIDATION
# ============================================================

def validate_feature_matrix(
    X: pd.DataFrame,
    name: str,
) -> None:

    print()
    print(
        f"========== {name} FEATURE CHECK =========="
    )

    forbidden = {
        "user_id",
        "post_id",
        "author_id",
        "impressed",
        "viewed",
        "liked",
        "saved",
        "commented",
        "shared",
        "completed",
        "skipped",
        "dwell_ms",
        "completion_ratio",
        "session_duration_seconds",
    }

    leaked_columns = (
        set(X.columns)
        & forbidden
    )

    if leaked_columns:
        raise ValueError(
            f"{name} contains forbidden/outcome "
            f"features: {sorted(leaked_columns)}"
        )

    expected = set(FEATURE_COLUMNS)

    actual = set(X.columns)

    missing = expected - actual

    if missing:
        raise ValueError(
            f"{name} missing features: "
            f"{sorted(missing)}"
        )

    unexpected = actual - expected

    if unexpected:
        raise ValueError(
            f"{name} contains unexpected features: "
            f"{sorted(unexpected)}"
        )

    print(
        f"[PASS] {len(X.columns)} pre-ranking features"
    )

    print(
        "[PASS] No raw identifiers"
    )

    print(
        "[PASS] No same-exposure outcomes"
    )


# ============================================================
# MAIN
# ============================================================

def main():

    print("=" * 60)
    print("POSTIFY RANKING MODEL V2")
    print("=" * 60)

    # --------------------------------------------------------
    # Load
    # --------------------------------------------------------

    print()
    print("Loading dataset...")

    df = pd.read_parquet(
        DATASET_PATH
    )

    print(
        f"Rows:     {len(df):,}"
    )

    print(
        f"Sessions: {df['session_id'].nunique():,}"
    )

    # --------------------------------------------------------
    # Contract validation
    # --------------------------------------------------------

    validate_dataset_contract(
        df,
        "FULL DATASET",
    )

    # --------------------------------------------------------
    # Build graded ranking target
    # --------------------------------------------------------

    df[TARGET_COLUMN] = (
        build_engagement_target(df)
    )

    print()
    print("========== TARGET DISTRIBUTION ==========")

    target_counts = (
        df[TARGET_COLUMN]
        .value_counts()
        .sort_index()
    )

    for label, count in target_counts.items():

        percentage = (
            count
            / len(df)
            * 100
        )

        print(
            f"{label} "
            f"({TARGET_LABELS[label]:<18}) "
            f"{count:>8,} "
            f"({percentage:6.2f}%)"
        )

    # --------------------------------------------------------
    # Session-safe temporal split
    # --------------------------------------------------------

    train_df, validation_df, test_df = (
        temporal_session_split(df)
    )

    print()
    print("========== TEMPORAL SPLIT ==========")

    print(
        f"Train:      {len(train_df):,} rows"
    )

    print(
        f"Validation: {len(validation_df):,} rows"
    )

    print(
        f"Test:       {len(test_df):,} rows"
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
    # Features
    # --------------------------------------------------------

    X_train = prepare_features(
        train_df
    )

    X_validation = prepare_features(
        validation_df
    )

    X_test = prepare_features(
        test_df
    )

    y_train = train_df[
        TARGET_COLUMN
    ]

    y_validation = validation_df[
        TARGET_COLUMN
    ]

    y_test = test_df[
        TARGET_COLUMN
    ]

    validate_feature_matrix(
        X_train,
        "X_TRAIN",
    )

    validate_feature_matrix(
        X_validation,
        "X_VALIDATION",
    )

    validate_feature_matrix(
        X_test,
        "X_TEST",
    )

    print()
    print("Feature count:", X_train.shape[1])

    print()
    print(
        "Features:"
    )

    for feature in X_train.columns:
        print(
            f"  {feature}"
        )

    # --------------------------------------------------------
    # Ranking groups
    # --------------------------------------------------------

    train_groups = get_session_groups(
        train_df
    )

    validation_groups = get_session_groups(
        validation_df
    )

    test_groups = get_session_groups(
        test_df
    )

    print()
    print("========== RANKING GROUPS ==========")

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
        f"{np.mean(train_groups):.2f}"
    )

    print(
        f"Median train candidates/session:  "
        f"{np.median(train_groups):.0f}"
    )

    # --------------------------------------------------------
    # Model
    # --------------------------------------------------------

    model = lgb.LGBMRanker(
        objective="lambdarank",

        metric="ndcg",

        ndcg_at=[5, 10, 20],

        n_estimators=2000,

        learning_rate=0.03,

        num_leaves=63,

        max_depth=-1,

        min_child_samples=50,

        subsample=0.85,

        subsample_freq=1,

        colsample_bytree=0.90,

        reg_alpha=0.20,

        reg_lambda=1.00,

        random_state=RANDOM_STATE,

        n_jobs=-1,

        verbosity=-1,
    )

    # --------------------------------------------------------
    # Training
    # --------------------------------------------------------

    print()
    print("========== TRAINING ==========")

    model.fit(
        X_train,
        y_train,

        group=train_groups,

        eval_set=[
            (
                X_validation,
                y_validation,
            )
        ],

        eval_group=[
            validation_groups
        ],

        eval_at=[
            5,
            10,
            20,
        ],

        callbacks=[
            lgb.early_stopping(
                EARLY_STOPPING_ROUNDS,
                verbose=True,
            ),

            lgb.log_evaluation(
                period=50,
            ),
        ],
    )

    print()
    print(
        f"Best iteration: "
        f"{model.best_iteration_}"
    )

    # --------------------------------------------------------
    # Evaluation
    # --------------------------------------------------------

    evaluate_ranker(
        model,
        X_validation,
        y_validation,
        validation_groups,
        "VALIDATION",
    )

    evaluate_ranker(
        model,
        X_test,
        y_test,
        test_groups,
        "TEST",
    )

    # --------------------------------------------------------
    # Feature importance
    # --------------------------------------------------------

    print()
    print(
        "========== FEATURE IMPORTANCE =========="
    )

    importance = (
        pd.DataFrame(
            {
                "feature": X_train.columns,

                "importance_gain": (
                    model.booster_
                    .feature_importance(
                        importance_type="gain"
                    )
                ),

                "importance_split": (
                    model.booster_
                    .feature_importance(
                        importance_type="split"
                    )
                ),
            }
        )
        .sort_values(
            "importance_gain",
            ascending=False,
        )
    )

    print(
        importance.to_string(
            index=False
        )
    )

    # --------------------------------------------------------
    # Save complete model artifact
    # --------------------------------------------------------
    #
    # Save the model together with the exact feature contract
    # and target definition. This prevents an inference service
    # from silently using a different feature order/definition.
    # --------------------------------------------------------

    MODEL_PATH.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    artifact = {
        "model": model,

        "model_version": "v2",

        "objective": "lambdarank",

        "feature_columns": list(
            X_train.columns
        ),

        "numeric_features": list(
            NUMERIC_FEATURES
        ),

        "categorical_features": list(
            CATEGORICAL_FEATURES
        ),

        "target_column": TARGET_COLUMN,

        "target_labels": dict(
            TARGET_LABELS
        ),

        "ranking_metrics": list(
            NDCG_K_VALUES
        ),

        "random_state": RANDOM_STATE,

        "best_iteration": (
            model.best_iteration_
        ),
    }

    joblib.dump(
        artifact,
        MODEL_PATH,
    )

    print()
    print(
        f"Model saved to:"
        f"\n{MODEL_PATH}"
    )

    print()
    print("=" * 60)
    print("POSTIFY RANKING MODEL V2 COMPLETE")
    print("=" * 60)


if __name__ == "__main__":
    main()