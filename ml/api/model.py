from __future__ import annotations

from pathlib import Path
from typing import Any

import numpy as np
import torch
import torch.nn as nn


class PostifyRankerV2(nn.Module):
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
                nn.LayerNorm(hidden_dim)
            )

            layers.append(
                nn.GELU()
            )

            layers.append(
                nn.Dropout(dropout)
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
            if isinstance(module, nn.Linear):
                nn.init.xavier_uniform_(
                    module.weight
                )

                if module.bias is not None:
                    nn.init.zeros_(
                        module.bias
                    )

            elif isinstance(module, nn.Embedding):
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

        embedded: list[torch.Tensor] = []

        for index, embedding in enumerate(
            self.embeddings
        ):
            embedded.append(
                embedding(
                    categorical_features[:, :, index]
                )
            )

        combined = [
            numeric_features,
            *embedded,
        ]

        x = torch.cat(
            combined,
            dim=-1,
        )

        return self.network(
            x
        ).squeeze(-1)


class RankerInference:
    """
    Loads the frozen Postify PyTorch V2 artifact and performs
    inference using the exact preprocessing contract from training.
    """

    MODEL_VERSION = "pytorch_v2"

    def __init__(
        self,
        model_path: Path,
        device: torch.device,
    ) -> None:

        if not model_path.exists():
            raise FileNotFoundError(
                f"Model artifact not found: {model_path}"
            )

        self.device = device
        self.model_path = model_path

        artifact = torch.load(
            model_path,
            map_location=device,
            weights_only=True,
        )

        self._validate_artifact(
            artifact
        )

        self.artifact = artifact

        self.numeric_features = (
            artifact["numeric_features"]
        )

        self.categorical_features = (
            artifact["categorical_features"]
        )

        self.feature_columns = (
            artifact["feature_columns"]
        )

        self.preprocessor = (
            artifact["preprocessor"]
        )

        categorical_cardinalities = (
            artifact["categorical_cardinalities"]
        )

        embedding_dims = (
            artifact["categorical_embedding_dims"]
        )

        architecture = artifact[
            "architecture"
        ]

        self.model = PostifyRankerV2(
            num_numeric_features=len(
                self.numeric_features
            ),
            categorical_cardinalities=(
                categorical_cardinalities
            ),
            categorical_embedding_dims=(
                embedding_dims
            ),
            hidden_dims=tuple(
                architecture["hidden_dims"]
            ),
            dropout=architecture["dropout"],
        ).to(device)

        self.model.load_state_dict(
            artifact["model_state_dict"]
        )

        self.model.eval()

    @staticmethod
    def _validate_artifact(
        artifact: dict[str, Any],
    ) -> None:

        required = {
            "model_version",
            "feature_columns",
            "numeric_features",
            "categorical_features",
            "preprocessor",
            "categorical_cardinalities",
            "categorical_embedding_dims",
            "architecture",
            "model_state_dict",
        }

        missing = required - set(
            artifact.keys()
        )

        if missing:
            raise ValueError(
                "Model artifact is missing required "
                f"fields: {sorted(missing)}"
            )

        if artifact["model_version"] != "pytorch_v2":
            raise ValueError(
                "Unsupported model version: "
                f"{artifact['model_version']}"
            )

    def predict(
        self,
        numeric_features: np.ndarray,
        categorical_features: np.ndarray,
    ) -> np.ndarray:

        if numeric_features.ndim != 2:
            raise ValueError(
                "numeric_features must have shape "
                "[candidates, features]."
            )

        if categorical_features.ndim != 2:
            raise ValueError(
                "categorical_features must have shape "
                "[candidates, features]."
            )

        if (
            numeric_features.shape[0]
            != categorical_features.shape[0]
        ):
            raise ValueError(
                "Numeric and categorical candidate counts "
                "must match."
            )

        if (
            numeric_features.shape[1]
            != len(self.numeric_features)
        ):
            raise ValueError(
                f"Expected "
                f"{len(self.numeric_features)} numeric "
                f"features, got "
                f"{numeric_features.shape[1]}."
            )

        if (
            categorical_features.shape[1]
            != len(self.categorical_features)
        ):
            raise ValueError(
                f"Expected "
                f"{len(self.categorical_features)} categorical "
                f"features, got "
                f"{categorical_features.shape[1]}."
            )

        numeric_tensor = torch.from_numpy(
            numeric_features.astype(
                np.float32
            )
        ).unsqueeze(0).to(
            self.device
        )

        categorical_tensor = torch.from_numpy(
            categorical_features.astype(
                np.int64
            )
        ).unsqueeze(0).to(
            self.device
        )

        with torch.inference_mode():
            scores = self.model(
                numeric_tensor,
                categorical_tensor,
            )

        return (
            scores[0]
            .detach()
            .cpu()
            .numpy()
        )

    def transform_numeric(
        self,
        feature_rows: list[dict[str, Any]],
    ) -> np.ndarray:

        output: list[list[float]] = []

        means = self.preprocessor[
            "numeric_mean"
        ]

        stds = self.preprocessor[
            "numeric_std"
        ]

        for row in feature_rows:
            values: list[float] = []

            for column in self.numeric_features:

                value = row.get(column)

                if value is None:
                    value = means[column]

                try:
                    value = float(value)
                except (
                    TypeError,
                    ValueError,
                ):
                    value = means[column]

                if not np.isfinite(value):
                    value = means[column]

                normalized = (
                    value - means[column]
                ) / stds[column]

                values.append(
                    normalized
                )

            output.append(values)

        return np.asarray(
            output,
            dtype=np.float32,
        )

    def transform_categorical(
        self,
        feature_rows: list[dict[str, Any]],
    ) -> np.ndarray:

        mappings = self.preprocessor[
            "category_mappings"
        ]

        output: list[list[int]] = []

        for row in feature_rows:
            values: list[int] = []

            for column in self.categorical_features:

                value = row.get(
                    column,
                    "UNKNOWN",
                )

                if value is None:
                    value = "UNKNOWN"

                value = str(value)

                mapping = mappings[column]

                encoded = mapping.get(
                    value,
                    0,
                )

                values.append(
                    encoded
                )

            output.append(values)

        return np.asarray(
            output,
            dtype=np.int64,
        )

    def rank(
        self,
        feature_rows: list[dict[str, Any]],
    ) -> np.ndarray:

        numeric = self.transform_numeric(
            feature_rows
        )

        categorical = self.transform_categorical(
            feature_rows
        )

        return self.predict(
            numeric,
            categorical,
        )