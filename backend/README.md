# Postify

A modern social media platform with ML-powered feed ranking, real-time messaging, and semantic search.

Built with **Spring Boot 3.4**, **MySQL**, **Kafka**, **Qdrant**, and an external **Python ML service**.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Local Development Setup](#local-development-setup)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [ML Service Integration](#ml-service-integration)
- [Database Migrations](#database-migrations)
- [Testing](#testing)
- [Docker Deployment](#docker-deployment)
- [CI/CD Pipeline](#ci-cd-pipeline)
- [Production Deployment](#production-deployment)
- [Project Structure](#project-structure)
- [Security](#security)
- [License](#license)

---

## Features

### Core Social Features
- **User authentication** with JWT tokens (HS256) and BCrypt password hashing
- **Post creation** with text content and optional image attachments (Cloudinary or local storage)
- **Like/unlike** posts with atomic, race-condition-free counter updates
- **Comments** on posts with nested reply support
- **Follow/unfollow** users with follower/following statistics
- **Profile management** with avatar upload, bio editing, and public statistics
- **Save/bookmark** posts for later reading
- **Real-time notifications** via WebSocket (likes, comments, follows)

### ML-Powered Feed
- **Personalized feed ranking** using an external Python ML service
- **Candidate generation** from multiple sources: following, interests, exploration
- **User interest tracking** via Kafka event streaming and topic feature extraction
- **Anonymous user support** with persistent interest profiles via cookies
- **Graceful degradation** when ML service is unavailable (deterministic fallback)

### Search & Discovery
- **Semantic search** using Qdrant vector database and LocalAI embeddings
- **Keyword search** for posts and users
- **Topic-based discovery** with AI-powered post analysis
- **Similar post recommendations** via vector similarity

### Real-Time Messaging
- **Direct messaging** between users via WebSocket (STOMP)
- **Conversation management** with read receipts and message previews
- **Online presence tracking** with connection status indicators

### Admin Panel
- **Dashboard** with system metrics, user statistics, and engagement analytics
- **Content moderation** with post hide/restore/delete capabilities
- **User management** with status updates and account details
- **Report handling** for user-reported content

### Infrastructure
- **Rate limiting** using Bucket4j with per-IP sliding window
- **Kafka event streaming** for interaction tracking and interest updates
- **Dead-letter topic** for failed Kafka messages
- **Vector sync queue** for reliable Qdrant embedding synchronization
- **Flyway migrations** for versioned database schema management

---

## Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend   │────▶│  Spring Boot │────▶│    MySQL     │
│  (React/TS)  │     │   Backend    │     │   Database   │
└──────────────┘     └──────┬───────┘     └──────────────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
        ┌─────▼─────┐ ┌────▼────┐ ┌──────▼──────┐
        │   Kafka   │ │ Qdrant  │ │  ML Service  │
        │  (Events) │ │ (Vector)│ │  (Python)    │
        └───────────┘ └─────────┘ └─────────────┘
              │
        ┌─────▼─────┐
        │  Cloud    │
        │inary      │
        │(Storage)  │
        └───────────┘
```

### Key Design Decisions
- **Atomic DB-level counter updates** — `UPDATE post SET like_count = like_count ± 1` prevents race conditions
- **AFTER_COMMIT event listeners** — notification failures never roll back the main transaction
- **Fire-and-forget Kafka with callbacks** — interaction events are logged on failure, not silently lost
- **HTTP timeouts on all external services** — ML client (5s), embedding service (10s), Qdrant (5s)
- **Graceful degradation** — feed and search work without ML/vector services

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Backend | Java 21, Spring Boot 3.4.0 |
| Security | Spring Security, JWT (jjwt 0.11.5) |
| Database | MySQL 8.0, Hibernate 6.6, Flyway 10.8 |
| Messaging | Apache Kafka, Spring Kafka |
| Vector DB | Qdrant (client 1.19.0) |
| Embeddings | LocalAI (OpenAI-compatible API) |
| ML Ranking | Python service (HTTP REST) |
| File Storage | Cloudinary / Local filesystem |
| Real-time | Spring WebSocket, STOMP |
| Rate Limiting | Bucket4j 8.14 |
| Observability | Spring Boot Actuator |
| Testing | JUnit 5, Mockito |
| Build | Maven |

---

## Prerequisites

- **JDK 21** (or later)
- **Maven 3.8+**
- **MySQL 8.0**
- **Apache Kafka** (for interaction events)
- **Qdrant** (for semantic search)
- **LocalAI** (for embeddings) — optional, search degrades gracefully
- **Python ML service** (for feed ranking) — optional, feed degrades gracefully
- **Cloudinary account** (for image storage) — or use local storage

---

## Local Development Setup

### 1. Clone and Build

```bash
git clone https://github.com/yourusername/Postify.git
cd Postify
./mvnw clean compile
```

### 2. Start Infrastructure

Start MySQL, Kafka, and Qdrant (see [Docker Deployment](#docker-deployment) for docker-compose).

### 3. Create Database

```sql
CREATE DATABASE postify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. Configure Environment

Create `src/main/resources/.env.properties`:

```properties
DB_URL=jdbc:mysql://localhost:3306/postify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=password
PORT=8080
JWT_SECRET=your-super-secret-key-at-least-256-bits
JWT_EXPIRATION_MS=86400000
ML_SERVICE_TOKEN=your-ml-token
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174
```

### 5. Run

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

---

## Configuration

All configuration is driven by environment variables. See `.env.example` for the complete list.

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | MySQL JDBC connection string | — |
| `DB_USERNAME` | Database user | — |
| `DB_PASSWORD` | Database password | — |
| `PORT` | HTTP server port | `8080` |
| `JWT_SECRET` | JWT signing secret (HS256) | — |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds | — |
| `ML_BASE_URL` | Python ML service URL | `http://localhost:8000` |
| `ML_SERVICE_TOKEN` | ML service authentication token | — |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | `localhost:9092` |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | — |
| `CLOUDINARY_API_KEY` | Cloudinary API key | — |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret | — |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:5173` |
| `STORAGE_BASE_PATH` | Local storage base directory | `./uploads` |

### Production Profile

Activate with `--spring.profiles.active=prod`. Production profile enforces:
- Secure cookies (`postify.cookie.secure=true`)
- No Flyway baseline (`baselineOnMigrate=false`)
- Strict HikariCP pool sizing
- Structured logging format

---

## API Endpoints

### Authentication (`/api/auth`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/register` | Register new user |
| POST | `/login` | Login and receive JWT token |

### Posts (`/api/posts`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create a new post |
| GET | `/` | List posts (paginated) |
| GET | `/{id}` | Get post details |
| PUT | `/{id}` | Update post content |
| DELETE | `/{id}` | Delete own post |

### Feed (`/api/feed`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Get personalized feed (ML-ranked) |

### Comments (`/api/comments`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Add comment to post |
| GET | `/{postId}` | Get comments for post |
| DELETE | `/{id}` | Delete own comment |

### Likes (`/api/likes`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/{postId}` | Toggle like on post |

### Follows (`/api/follow`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/{userId}` | Follow user |
| DELETE | `/{userId}` | Unfollow user |

### Search (`/api/search`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/posts` | Search posts (keyword + semantic) |
| GET | `/users` | Search users by username |

### Notifications (`/api/notifications`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Get user notifications (paginated, `?page=0&size=20`) |
| PUT | `/read` | Mark all as read |
| PUT | `/{id}/read` | Mark single as read |

### Profile (`/api/profile`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/me` | Get current user profile |
| PUT | `/me` | Update profile |
| GET | `/{username}` | Get public profile |
| GET | `/{username}/posts` | Get user's posts |
| GET | `/{username}/followers` | Get followers |
| GET | `/{username}/following` | Get following |

### Saved Posts (`/api/saved`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/{postId}` | Save post |
| DELETE | `/{postId}` | Unsave post |
| GET | `/` | Get saved posts |

### Conversations (`/api/conversations`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Get user's conversations |
| GET | `/{id}` | Get conversation details |

### Messages (`/api/messages`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Send message (HTTP fallback) |

### Admin (`/api/admin`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/dashboard` | Dashboard metrics |
| GET | `/users` | List users (paginated) |
| GET | `/users/{id}` | User details |
| PATCH | `/users/{id}/status` | Update user status |
| GET | `/posts` | List posts (with filters) |
| PATCH | `/posts/{id}/hide` | Hide post |
| PATCH | `/posts/{id}/restore` | Restore post |
| DELETE | `/posts/{id}` | Delete post |

### WebSocket

| Endpoint | Description |
|----------|-------------|
| `/ws` | STOMP endpoint for real-time messaging and notifications |

---

## ML Service Integration

The feed ranking system integrates with an external Python ML service:

1. **Candidate Generation**: Posts are gathered from following, interests, and exploration sources
2. **Feature Building**: User engagement history, post freshness, and topic features are extracted
3. **ML Scoring**: Candidates are sent to the ML service for ranking scores
4. **Fallback**: If the ML service is unavailable, a deterministic fallback ranks by recency and engagement

The ML service communicates via HTTP REST with a 5-second timeout. Failures are logged and the feed degrades gracefully.

---

## Database Migrations

Schema is managed by **Flyway** with 29 incremental migrations:

- `V001` — Initial schema (users, posts, likes, comments, follows) — aligned with JPA entities
- `V003-V005` — Notifications system
- `V006-V007` — Topics and post-topic mapping
- `V008-V010` — Post media table (migrated from image_url)
- `V011-V013` — Interaction events, user interactions, user interests
- `V015-V016` — User topic features for interest tracking
- `V017-V019` — Anonymous user support
- `V020-V024` — Messaging system with read receipts
- `V025` — Anonymous user retention
- `V026-V028` — Vector sync queue and post analysis queue

Migrations run automatically on startup. In production, `baselineOnMigrate=false` ensures explicit migration control.

---

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=AuthServiceTest

# Run with coverage (requires JaCoCo plugin)
./mvnw test jacoco:report
```

The test suite covers:
- Authentication and authorization flows
- Post creation and management
- Feed ranking and fallback behavior
- WebSocket authentication
- Rate limiting
- Ranking feature building
- ML client behavior
- Admin content moderation
- Message and conversation services
- Anonymous user handling

---

## Docker Deployment

### Dockerfile

A multi-stage Dockerfile is provided for production builds:

```bash
# Build production image
docker build -t postify:latest .

# Run with environment variables
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://db:3306/postify \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=password \
  -e JWT_SECRET=your-secret \
  postify:latest
```

### Docker Compose

Full stack deployment with all dependencies:

```bash
docker-compose up -d
```

Services included:
- **Postify backend** (Spring Boot)
- **MySQL 8.0** (persistent data volume)
- **Kafka + Zookeeper** (event streaming)
- **Qdrant** (vector search)
- **LocalAI** (embeddings)

See `docker-compose.yml` for full configuration.

---

## CI/CD Pipeline

GitHub Actions automate build, test, and deployment:

### Workflow Triggers
- **Push to `main`**: Build, test, security scan, deploy to staging
- **Pull requests**: Build, test, code quality checks
- **Scheduled (daily)**: Dependency update check

### Pipeline Stages
1. **Build**: Compile with Maven, cache dependencies
2. **Test**: Run full test suite
3. **Security**: OWASP dependency check for known vulnerabilities
4. **Docker Build**: Build and push container image
5. **Deploy**: Deploy to production (manual approval gate)

See `.github/workflows/ci-cd.yml` for the full pipeline configuration.

---

## Production Deployment

### 1. Build and Package

```bash
./mvnw clean package -Pprod -DskipTests
```

### 2. Required Environment Variables

All sensitive configuration must be provided via environment variables. See `.env.example` for the complete list.

### 3. Run with Production Profile

```bash
java -jar target/postify-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 4. Health Checks

Spring Boot Actuator exposes health endpoints:
- `GET /actuator/health` — Overall health status
- `GET /actuator/health/readiness` — Kubernetes readiness probe
- `GET /actuator/health/liveness` — Kubernetes liveness probe

### 5. Monitoring

- **Metrics**: Exposed via `/actuator/prometheus` (requires Micrometer Prometheus registry)
- **Logging**: Pattern-based logging in development, configurable via environment
- **HikariCP**: Connection leak detection at 10-second threshold

---

## Project Structure

```
src/
├── main/
│   ├── java/com/omar/postify/
│   │   ├── config/           # Application configuration
│   │   │   ├── KafkaConfig.java
│   │   │   ├── RestClientConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebSocketConfig.java
│   │   ├── controller/       # REST API endpoints (15 controllers)
│   │   ├── dto/              # Data transfer objects
│   │   │   ├── admin/        # Admin-specific DTOs
│   │   │   ├── enums/        # Enumerations
│   │   │   └── metrics/      # Dashboard metrics DTOs
│   │   ├── entities/         # JPA entities
│   │   ├── event/            # Application events
│   │   │   └── NotificationEventListener.java
│   │   ├── exception/        # Custom exceptions and handlers
│   │   ├── rateLimit/        # Rate limiting (Bucket4j)
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── security/         # JWT filter, WebSocket auth
│   │   ├── service/          # Business logic (54 services)
│   │   │   ├── admin/        # Admin panel services
│   │   │   ├── ai/           # AI and vector services
│   │   │   ├── feed/         # Feed ranking and candidates
│   │   │   ├── kafka/        # Kafka producers and consumers
│   │   │   └── storage/      # File storage abstraction
│   │   └── util/             # Utilities (JWT, request identity)
│   └── resources/
│       ├── db/migration/     # Flyway migrations (29 files)
│       ├── application.properties
│       └── application-prod.properties
└── test/
    └── java/com/omar/postify/  # Test classes (31 files)
```

---

## Security

- **JWT authentication** with HS256 signing
- **BCrypt password hashing** for stored credentials
- **CORS** configured for specific origins
- **Rate limiting** on authentication and content creation endpoints
- **CSRF disabled** for stateless JWT API (frontend handles CSRF separately)
- **Input validation** via Bean Validation and service-layer checks
- **Database-level uniqueness** constraints on username and email
- **Secure cookies** in production (`postify.cookie.secure=true`)
- **Admin role enforcement** via `@PreAuthorize` and role-based access control

---

## License

MIT

---

## Contact

Email: 3mur1111@gmail.com
