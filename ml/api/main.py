from __future__ import annotations

from contextlib import asynccontextmanager
from datetime import datetime, timezone
from pathlib import Path
import hmac
import os
from typing import Any

import numpy as np
import torch
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from api.model import RankerInference
from api.schemas import (
    RankRequest,
    RankResponse,
    RankedCandidate,
)

# Load .env file so that ML_SERVICE_TOKEN and other secrets are available
PROJECT_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(PROJECT_ROOT / ".env")

MODEL_PATH = (
    PROJECT_ROOT
    / "models"
    / "postify_ranker_pytorch_v2.pt"
)

DEVICE = torch.device(
    "cuda"
    if torch.cuda.is_available()
    else "cpu"
)


class ApplicationState:
    ranker: RankerInference | None = None


state = ApplicationState()

SERVICE_TOKEN_ENV = "ML_SERVICE_TOKEN"


def is_production_environment() -> bool:
    production_environments = {"prod", "production"}
    return any(
        os.getenv(name, "").strip().lower() in production_environments
        for name in ("APP_ENV", "ENVIRONMENT")
    )


def validate_service_auth_configuration() -> None:
    if is_production_environment() and not os.getenv(SERVICE_TOKEN_ENV, "").strip():
        raise RuntimeError(
            "ML service authentication must be configured in production."
        )


@asynccontextmanager
async def lifespan(
    app: FastAPI,
):
    validate_service_auth_configuration()

    print("=" * 60)
    print("POSTIFY ML SERVICE")
    print("=" * 60)

    print(
        f"Device: {DEVICE}"
    )

    if torch.cuda.is_available():
        print(
            "GPU: "
            f"{torch.cuda.get_device_name(0)}"
        )

    print(
        f"Loading model:\n{MODEL_PATH}"
    )

    try:
        state.ranker = RankerInference(
            model_path=MODEL_PATH,
            device=DEVICE,
        )
    except Exception as exc:
        print(
            "FATAL: Failed to load ranking model."
        )
        raise RuntimeError(
            "ML service cannot start because "
            "the PyTorch model failed to load."
        ) from exc

    print(
        "Model loaded successfully."
    )

    print("=" * 60)

    yield

    print(
        "Shutting down Postify ML service."
    )

    state.ranker = None


app = FastAPI(
    title="Postify ML Service",
    description=(
        "PyTorch V2 recommendation ranking "
        "service for Postify."
    ),
    version="2.0.0",
    lifespan=lifespan,
    docs_url=None if is_production_environment() else "/docs",
    redoc_url=None if is_production_environment() else "/redoc",
    openapi_url=None if is_production_environment() else "/openapi.json",
)


@app.middleware("http")
async def authorize_rank_request(request: Request, call_next):
    if request.url.path == "/rank" and request.method == "POST":
        configured_token = os.getenv(SERVICE_TOKEN_ENV, "")
        if not configured_token.strip():
            return JSONResponse(
                status_code=503,
                content={"detail": "Ranking service authentication is not configured."},
            )

        supplied_token = request.headers.get("X-ML-Service-Token", "")
        if not supplied_token or not hmac.compare_digest(
            supplied_token.encode("utf-8"), configured_token.encode("utf-8")
        ):
            return JSONResponse(
                status_code=401,
                content={"detail": "Invalid service credentials."},
            )

    return await call_next(request)


@app.get(
    "/health",
)
def health() -> dict[str, Any]:

    return {
        "status": "ok",

        "service": "postify-ml",

        "model": (
            "pytorch_ranker_v2"
        ),

        "device": str(
            DEVICE
        ),

        "cuda_available": (
            torch.cuda.is_available()
        ),
    }


@app.get(
    "/ready",
)
def readiness() -> dict[str, Any]:

    if state.ranker is None:
        raise HTTPException(
            status_code=503,
            detail="Ranking model is not loaded.",
        )

    return {
        "status": "ready",

        "model": (
            "pytorch_ranker_v2"
        ),
    }
@app.post(
    "/rank",
    response_model=RankResponse,
)
def rank(
    request: RankRequest,
) -> RankResponse:

    if state.ranker is None:
        raise HTTPException(
            status_code=503,
            detail="Ranking model is not loaded.",
        )

    candidates = request.candidates

    if not candidates:
        raise HTTPException(
            status_code=400,
            detail="At least one candidate is required.",
        )

    # --------------------------------------------------------
    # Build model feature rows.
    #
    # post_age_hours, session_hour, and
    # session_day_of_week are derived here, exactly like
    # training.
    # --------------------------------------------------------

    feature_rows: list[
        dict[str, Any]
    ] = []

    session_started = (
        request.session_started_at
    )

    if (
        session_started.tzinfo is None
    ):
        session_started = (
            session_started.replace(
                tzinfo=timezone.utc
            )
        )

    for candidate in candidates:

        post_created = (
            candidate.post_created_at
        )

        if post_created.tzinfo is None:
            post_created = (
                post_created.replace(
                    tzinfo=timezone.utc
                )
            )

        age_hours = (
            session_started
            - post_created
        ).total_seconds() / 3600.0

        if age_hours < 0:
            raise HTTPException(
                status_code=422,
                detail=(
                    "Candidate post creation time "
                    "cannot be after session start."
                ),
            )

        feature_rows.append(
            {
                "user_activity_level":
                    candidate.user_activity_level,

                "user_session_length_mean":
                    candidate.user_session_length_mean,

                "user_exploration_rate":
                    candidate.user_exploration_rate,

                "user_engagement_bias":
                    candidate.user_engagement_bias,

                "history_impressions":
                    candidate.history_impressions,

                "history_views":
                    candidate.history_views,

                "history_likes":
                    candidate.history_likes,

                "history_saves":
                    candidate.history_saves,

                "history_comments":
                    candidate.history_comments,

                "history_shares":
                    candidate.history_shares,

                "history_like_rate":
                    candidate.history_like_rate,

                "history_save_rate":
                    candidate.history_save_rate,

                "history_comment_rate":
                    candidate.history_comment_rate,

                "history_share_rate":
                    candidate.history_share_rate,

                "history_engagement_rate":
                    candidate.history_engagement_rate,

                "history_avg_dwell_ms":
                    candidate.history_avg_dwell_ms,

                "history_avg_completion":
                    candidate.history_avg_completion,

                "duration_ms":
                    candidate.duration_ms,

                "topic_count":
                    candidate.topic_count,

                "topic_relevance":
                    candidate.topic_relevance,

                "post_age_hours":
                    age_hours,

                "session_hour":
                    session_started.hour,

                "session_day_of_week":
                    session_started.weekday(),

                "content_type":
                    candidate.content_type,

                "media_type":
                    (
                        candidate.media_type
                        or "UNKNOWN"
                    ),
            }
        )

    try:
        scores = state.ranker.rank(
            feature_rows
        )
    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=(
                "Ranking inference failed."
            ),
        ) from exc

    if len(scores) != len(candidates):
        raise HTTPException(
            status_code=500,
            detail=(
                "Model returned an unexpected "
                "number of scores."
            ),
        )

    ranked = sorted(
        zip(
            candidates,
            scores,
        ),
        key=lambda item: float(
            item[1]
        ),
        reverse=True,
    )

    ranked_candidates = [
        RankedCandidate(
            post_id=candidate.post_id,
            score=float(score),
            rank=index,
        )
        for index, (
            candidate,
            score,
        ) in enumerate(
            ranked,
            start=1,
        )
    ]

    return RankResponse(
        model_version=(
            "pytorch_ranker_v2"
        ),

        candidate_count=len(
            candidates
        ),

        ranked_candidates=(
            ranked_candidates
        ),
    )


if __name__ == "__main__":
    import uvicorn
    # Starts the server on localhost port 8000
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)
