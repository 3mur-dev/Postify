
content=Great+post
```

Response: JSON payload containing the comment id, author, avatar, content, and timestamp.

### Toggle a Like

```http
POST /posts/42/like
```

Response:

```json
{
  "liked": true,
  "count": 17
}
```

### Subscribe to Notifications

```http
GET /notifications/stream
```

This endpoint returns an SSE stream for the authenticated user.

## Environment Variables

Configured through `.env`, imported with `spring.config.import=optional:file:.env`.

```properties
DB_URL=jdbc:mysql://localhost:3306/postify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=root
PORT=8080
JWT_SECRET=your_jwt_secret_here
JWT_EXPIRATION_MS=86400000
```

Notes:

- `JWT_SECRET` and `JWT_EXPIRATION_MS` are available in configuration for the token utility layer.
- Current runtime authentication is still handled by Spring Security form login.

## Database

- Schema is managed with Flyway migrations in `src/main/resources/db/migration`.
- `spring.jpa.hibernate.ddl-auto=validate` keeps the application honest against the migration history.
- Hibernate SQL logging is enabled for local development.
- Tests use H2 with a dedicated test profile.

## Running Locally

### Prerequisites

- JDK 21
- Maven or Maven Wrapper
- MySQL 8+ running locally

### Start the App

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

- Upload directory: `uploads/` (git-ignored runtime data)
- Avatar and post images are persisted on disk and exposed through configured web paths
  macOS / Linux:

```bash
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8080
```

## Testing

Run the full test suite:

Windows:

```powershell
.\mvnw.cmd test
```

macOS / Linux:

```bash
./mvnw test
```

The test suite includes:

- Controller tests using `@WebMvcTest` and MockMvc
- Integration tests using `@SpringBootTest` with H2

## Project Structure

```text
      service/
      util/
    resources/
      templates/
      db/migration/
      static/
      templates/
      application.properties
  test/
    java/com/omar/postify/
      controller/
      integration/
    resources/
      application-test.properties
uploads/
```

## Notes

- Avatar uploads are served from `/images/avatars/**`.
- Post creation and profile editing both support multipart uploads.
- Admin activity is recorded in the `admin_logs` domain model.
- A `JwtUtil` helper exists in the codebase, but it is not currently wired into the active security chain.

## Contact

`3mur1111@gmail.com`