# Postify ML Service — Feed Ranking & Topic Classification

[![Python](https://img.shields.io/badge/Python-3.10+-blue.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.141-009688.svg)](https://fastapi.tiangolo.com/)
[![PyTorch](https://img.shields.io/badge/PyTorch-2.13-ee4c2c.svg)](https://pytorch.org/)
[![Tests](https://img.shields.io/badge/Tests-5%20passing-success.svg)]()

Python ML microservice for the Postify social media platform. Provides feed ranking via a neural network model, topic classification, and text embeddings.

## ✨ Features

| Feature | Description |
|---|---|
| **Feed Ranking** | Neural network (PyTorch) ranks post candidates by predicted engagement score |
| **Topic Classification** | Analyzes post content and assigns topics with confidence scores |
| **Text Embeddings** | Generates vector embeddings for semantic search via Qdrant |
| **Temporal Safety** | User history builder ensures features use only pre-interaction data — no temporal leakage |
| **Service Authentication** | HMAC token authentication on all ranking endpoints |
| **Input Validation** | Pydantic schemas with `extra="forbid"` prevent unexpected fields |
| **Health Checks** | Unauthenticated `/health` and `/ready` endpoints for load balancers |

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  Postify ML Service                     │
│                  (FastAPI + Uvicorn)                    │
│                                                         │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │   API Layer │  │  Model Layer │  │  Data Layer   │  │
│  │             │  │              │  │               │  │
│  │ POST /rank  │─▶│ PyTorch NN   │  │ Feature build │  │
│  │ POST /topic │─▶│ Topic model  │  │ History accum │  │
│  │ POST /embed │─▶│ Embedding    │  │ Temporal safe │  │
│  │ GET /health │  │ model        │  │               │  │
│  │ GET /ready  │  │              │  │               │  │
│  └─────────────┘  └──────────────┘  └───────────────┘  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │         Training Pipeline (offline)              │   │
│  │                                                  │   │
│  │  Simulator → Dataset → Features → Train → Eval   │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### Technology Stack

| Component | Technology |
|---|---|
| API Framework | FastAPI 0.141 |
| ASGI Server | Uvicorn 0.52 |
| ML Framework | PyTorch 2.13 (CUDA 13) |
| Data Processing | Pandas 3.0, NumPy 2.5, PyArrow 25 |
| Validation | Pydantic 2.13 |
| ML Tools | scikit-learn 1.9, LightGBM 4.7, scipy 1.18 |
| Database | SQLAlchemy 2.0, PyMySQL 1.2 |
| Configuration | python-dotenv 1.2 |

## 🚀 Quick Start

### Prerequisites

- **Python 3.10+**
- **pip** (for dependency installation)

### 1. Create a Virtual Environment

```bash
python -m venv .venv
.venv\Scripts\activate    # Windows
# source .venv/bin/activate    # Linux/Mac
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Configure Service Token

The `/rank` endpoint requires HMAC token authentication. Generate a random secret:

```powershell
# PowerShell
$serviceToken = [Convert]::ToBase64String(
  [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
)
$env:ML_SERVICE_TOKEN = $serviceToken
$env:APP_ENV = "development"
```

```bash
# Bash
export ML_SERVICE_TOKEN=$(openssl rand -base64 32)
export APP_ENV="development"
```

Set the same value as `POSTIFY_ML_SERVICE_TOKEN` in the Spring Boot backend environment.

> ⚠️ **Security**: Never commit `.env` files. The `.gitignore` already excludes `.env`, `.venv/`, `__pycache__/`, `output/`.

### 4. Start the Service

```bash
.venv\Scripts\python.exe -m uvicorn api.main:app --host 127.0.0.1 --port 8000
```

The service starts on `http://127.0.0.1:8000`. Health checks are available at `/health` and `/ready`.

## 📡 API Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/rank` | Token | Rank post candidates for a user's feed |
| `POST` | `/topic` | Token | Classify topics for post content |
| `POST` | `/embed` | Token | Generate text embeddings |
| `GET` | `/health` | None | Health check for load balancers |
| `GET` | `/ready` | None | Readiness check (model loaded, dependencies ready) |

### Ranking Request

```json
{
  "user_id": "abc123",
  "candidates": [
    { "post_id": "post-1", "author_id": "user-1", "topic": "technology" },
    { "post_id": "post-2", "author_id": "user-2", "topic": "sports" }
  ]
}
```

### Ranking Response

```json
{
  "rankings": [
    { "post_id": "post-1", "score": 0.85 },
    { "post_id": "post-2", "score": 0.42 }
  ]
}
```

## 🔒 Service Authentication

The `POST /rank`, `POST /topic`, and `POST /embed` endpoints require the shared service credential in the `X-ML-Service-Token` request header.

- **Missing token**: Returns `503 Service Unavailable`
- **Invalid token**: Returns `401 Unauthorized`
- **Token comparison**: Uses HMAC timing-safe comparison to prevent timing attacks

The credential is a backend-to-backend secret. Never expose it in frontend code, source control, request bodies, or logs.

### Production Requirements

- Set `APP_ENV=production` — startup fails if `ML_SERVICE_TOKEN` is absent
- Use HTTPS for all ML service communication (enforced by the Spring Boot backend)
- Deploy behind a firewall or ingress rule — token auth does not provide network isolation
- Use a strong, randomly generated token (≥256 bits)

## 🧠 Model Architecture

### Feed Ranking Model

- **Type**: Neural network (PyTorch) with embedding layers + MLP
- **Input**: User-interest features, post features, interaction history
- **Output**: Engagement score (0.0–1.0) for each candidate post
- **Training data**: Behavioral simulation engine with topic-based engagement patterns
- **Evaluation**: NDCG and Recall on temporal holdout sessions

### Temporal Leakage Prevention

The `UserHistoryBuilder` accumulates features only from interactions that occurred **before** each prediction point. The `HistoricalLeakageChecker` validates that pre-interaction history matches cumulative sums, ensuring no future data leaks into training features.

### Training Pipeline

```
Simulator → Dataset → Features → Train → Evaluate
```

1. **Simulator** (`simulator/`) — Generates synthetic users, posts, and interaction sessions with behavioral models
2. **Dataset** (`training/dataset.py`) — Constructs temporal session-based train/val/test split
3. **Features** (`training/features.py`) — Builds user-interest and post features from interaction history
4. **Train** (`training/train_pytorch.py`) — Trains the PyTorch neural network with early stopping
5. **Evaluate** (`training/analysis.py`) — Computes NDCG, Recall, and other ranking metrics

## 🧪 Testing

```bash
# Run all tests
.venv\Scripts\python.exe -m pytest tests/ -v

# Run a specific test file
.venv\Scripts\python.exe -m pytest tests/test_rank_auth.py -v
```

### Test Coverage

| Test File | Purpose |
|---|---|
| `test_rank_auth.py` | Service token authentication, HMAC validation, error responses |
| `test_dataset_temporal.py` | Temporal leakage checks, train/val/test separation |

**Total: 5 tests, 0 failures.**

## ⚠️ Known Limitations

- **Synthetic training data**: The ranking model is trained on simulated user interaction data generated by a behavioral simulation engine. While the simulation includes topic-based engagement patterns and temporal session structure, the model has never been trained on real user data. Reported evaluation metrics (NDCG, Recall) are computed against synthetic labels and should not be interpreted as evidence of real-world ranking improvement.
- **No A/B testing**: Ranking quality has not been evaluated against real user behavior.
- **Single model**: No model ensemble or fallback to a different ranking strategy.
- **Batch inference only**: The `/rank` endpoint processes all candidates in a single request. Very large candidate sets may increase latency.

## 📁 Project Structure

```
postify-ml/
├── api/                   # FastAPI application
│   ├── main.py            # App entry point, routes, middleware
│   ├── model.py           # Model loading and inference
│   └── schemas.py         # Pydantic request/response schemas
├── data/                  # Data access layer
│   ├── database.py        # Database connection
│   ├── post_repository.py # Post data queries
│   └── topic_repository.py # Topic data queries
├── simulator/             # Behavioral simulation engine
│   ├── behavior.py        # User behavior models
│   ├── config.py          # Simulation configuration
│   ├── events.py          # Interaction event generation
│   ├── feed.py            # Feed simulation
│   ├── posts.py           # Synthetic post generation
│   ├── sessions.py        # Session structure
│   ├── simulation.py      # Simulation orchestration
│   └── users.py           # Synthetic user generation
├── training/              # Training pipeline
│   ├── dataset.py         # Dataset construction and splitting
│   ├── features.py        # Feature engineering
│   ├── user_history.py    # Temporal-safe history builder
│   ├── train_pytorch.py   # PyTorch model training
│   ├── train.py           # Legacy training script
│   ├── analysis.py        # Evaluation metrics
│   ├── leakage_check.py   # Temporal leakage validation
│   └── predict.py         # Batch prediction utility
├── tests/                 # Test suite
│   ├── test_rank_auth.py  # Authentication tests
│   └── test_dataset_temporal.py  # Temporal safety tests
├── scripts/               # Utilities
│   └── smoke_spring_ml.py # Spring Boot ↔ ML smoke test
├── main.py                # Entry point for simulation
├── requirements.txt       # Python dependencies
└── .env                   # Local environment (gitignored)
```

## 📦 Deployment

### Environment Variables

| Variable | Required | Description |
|---|---|---|
| `ML_SERVICE_TOKEN` | Yes (prod) | HMAC secret for `/rank` authentication |
| `APP_ENV` | Recommended | `development` or `production` |

### Docker

A Dockerfile is not yet provided. For containerized deployment, use a Python 3.10+ base image, install dependencies from `requirements.txt`, and set `ML_SERVICE_TOKEN` via environment variables.

### Health Checks

Configure your load balancer or orchestrator to use:

- `GET /health` — Basic liveness check
- `GET /ready` — Readiness check (model loaded, dependencies ready)

Both endpoints are unauthenticated and return `200 OK` when the service is healthy.

## 📚 Related Repositories

- **[Postify](https://github.com/your-username/Postify)** — Spring Boot backend (REST API, business logic)
- **[postify-web](https://github.com/your-username/postify-web)** — React 19 + TypeScript frontend

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.
