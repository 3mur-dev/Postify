# check_leakage.py
from __future__ import annotations
import pandas as pd


class HistoricalLeakageChecker:
    def __init__(self, history_columns: list[str]):
        self.history_columns = history_columns

    def verify(self, df: pd.DataFrame) -> bool:
        print("=" * 60)
        print("HISTORICAL LEAKAGE CHECK")
        print("=" * 60)

        failures = 0

        # 1. Required column validation
        required = ["user_id", "liked", "saved", "commented", "shared"] + self.history_columns
        missing = [c for c in required if c not in df.columns]
        if missing:
            print(f"[FAIL] Missing columns: {missing}")
            return False

        print("[PASS] All required columns exist")

        # 2. Lower-bound boundary evaluation
        for column in self.history_columns:
            invalid = (df[column].fillna(0) < 0).sum()
            if invalid:
                print(f"[FAIL] {column}: {invalid} negative values")
                failures += invalid
            else:
                print(f"[PASS] {column}: no negative values")

        # 3. Ground truth mathematical comparisons
        expected_likes = df.groupby("user_id")["liked"].cumsum().sub(df["liked"])
        expected_views = df.groupby("user_id")["viewed"].cumsum().sub(df["viewed"])
        expected_saves = df.groupby("user_id")["saved"].cumsum().sub(df["saved"])
        expected_shares = df.groupby("user_id")["shared"].cumsum().sub(df["shared"])

        pairs = [
            ("history_likes", expected_likes),
            ("history_views", expected_views),
            ("history_saves", expected_saves),
            ("history_shares", expected_shares)
        ]

        for history_col, expected in pairs:
            if history_col in df.columns:
                actual = df[history_col].fillna(0)
                invalid = (actual - expected).abs() > 1e-9
                count = int(invalid.sum())

                if count:
                    print(f"[FAIL] {history_col}: {count:,} rows do not match pre-interaction history")
                    failures += count
                else:
                    print(f"[PASS] {history_col}: matches pre-interaction history")

        print("=" * 60)
        if failures:
            print(f"HISTORICAL LEAKAGE CHECK: FAILED ({failures} problems)")
            return False

        print("HISTORICAL LEAKAGE CHECK: PASSED (0 problems) 🎉")
        return True


def main():
    import pandas as pd

    # Load your dataset to test it
    DATASET_PATH = "output/large_test_dataset.parquet"
    print(f"Loading {DATASET_PATH} for verification...")
    df = pd.read_parquet(DATASET_PATH)

    # Define columns to check
    HISTORY_COLUMNS = [
        "history_impressions", "history_views", "history_likes", "history_saves",
        "history_comments", "history_shares", "history_like_rate", "history_save_rate",
        "history_comment_rate", "history_share_rate", "history_engagement_rate",
        "history_avg_dwell_ms", "history_avg_completion",
    ]

    # Run the check
    checker = HistoricalLeakageChecker(HISTORY_COLUMNS)
    checker.verify(df)


# THIS IS THE CRITICAL MISSING LINK FOR TERMINAL EXECUTION:
if __name__ == "__main__":
    main()