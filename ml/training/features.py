from __future__ import annotations

import pandas as pd


TARGET_COLUMNS = {
    "dwell_ms",
    "completion_ratio",
    "completed",
    "skipped",
    "liked",
    "saved",
    "commented",
    "shared",
}


ID_COLUMNS = {
    "session_id",
}


CATEGORICAL_COLUMNS = {
    "content_type",
    "media_type",
}


DROP_COLUMNS = {
    "post_created_at",
    "session_started_at",
    # impressed_at hour/dow would duplicate session_hour / session_day_of_week
    "impressed_at",
}


def prepare_features(
    df: pd.DataFrame,
    target: str = "liked",
) -> tuple[pd.DataFrame, pd.Series]:

    if target not in df.columns:
        raise ValueError(f"Target column '{target}' does not exist.")

    if target in TARGET_COLUMNS and target != "liked":
        raise ValueError(
            f"Target '{target}' is not supported by this training pipeline yet."
        )

    data = df.copy()

    y = data[target].astype("int8")

    # Remove every outcome column, including the training target.
    columns_to_drop = (
        TARGET_COLUMNS
        | ID_COLUMNS
        | DROP_COLUMNS
    )

    X = data.drop(
        columns=[
            column
            for column in columns_to_drop
            if column in data.columns
        ]
    )

    # Convert categorical values to pandas categorical dtype.
    for column in CATEGORICAL_COLUMNS:
        if column in X.columns:
            X[column] = X[column].astype("category")

    # Convert timestamps into useful numeric features.
    if "post_created_at" in data.columns:
        post_time = pd.to_datetime(
            data["post_created_at"],
            errors="coerce"
        )

        X["post_age_hours"] = (
            (
                pd.to_datetime(data["session_started_at"], errors="coerce")
                - post_time
            ).dt.total_seconds()
            / 3600.0
        )

    if "session_started_at" in data.columns:
        session_time = pd.to_datetime(
            data["session_started_at"],
            errors="coerce"
        )

        X["session_hour"] = session_time.dt.hour
        X["session_day_of_week"] = session_time.dt.dayofweek

    # Remove any remaining raw timestamp columns (datetime64 / datetimetz).
    datetime_columns = X.select_dtypes(
        include=["datetime", "datetimetz"]
    ).columns.tolist()

    if datetime_columns:
        X = X.drop(columns=datetime_columns)

    # Ensure numeric values are finite.
    numeric_columns = X.select_dtypes(
        include=["number"]
    ).columns

    X[numeric_columns] = (
        X[numeric_columns]
        .replace([float("inf"), float("-inf")], pd.NA)
    )

    return X, y
