# Postify

Postify is a Spring Boot + Thymeleaf social media web app where users can register, log in, publish posts, upload images, like/comment/follow, edit profiles, and browse user/content pages.

## Core Features

- Authentication with Spring Security form login (`/login`, `/logout`)
- User registration with validation (`/register`, `/register/add`)
- Home feed with keyword search and pagination (`/`)
- Create and delete posts with optional image upload (`/posts/create`, `/posts/delete/{postId}`)
- Comment APIs per post (`GET/POST /posts/{postId}/comments`)
- Like toggle API per post (`POST /posts/{postId}/like`)
- Follow/unfollow users (`POST /follow/{username}`)
- Profile page and profile editing with avatar upload (`/profile/{username}`, `/profile/edit`)
- User search by username (`/search?q=...`)
- Admin dashboard, user management, and admin logs (`/admin/**`)
- Real-time notification stream via Server-Sent Events (`/notifications/stream`)

## Tech Stack

- Java 21
- Spring Boot 4
- Spring MVC + Thymeleaf
- Spring Security
- Spring Data JPA (Hibernate)
- MySQL (runtime database)
- H2 (test database)
- JUnit 5 + Spring Test + MockMvc
- Lombok

## Architecture

- `controller`: HTTP endpoints (web pages + JSON APIs)
- `service`: business logic (auth, posts, comments, likes, profile, notifications, admin logs)
- `repository`: JPA repositories
- `entities`: domain model (`User`, `Post`, `Comment`, `Like`, `Follow`, `AdminLog`, `Role`)
- `security`: security filter chain + authentication setup
- `templates`: Thymeleaf pages and fragments

## Security Rules (Current)

- Public routes include `/`, `/login`, `/register`, `/profile/**`, `/search/**`, and static assets
- `/admin/**` requires `ROLE_ADMIN`
- All other routes require authentication
- CSRF is currently disabled in `SecurityConfig`

## Environment Variables

Configured through `.env` loaded by `spring.config.import=optional:file:.env`.

- `DB_URL` (default: `jdbc:mysql://localhost:3306/postify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
- `DB_USERNAME` (default: `root`)
- `DB_PASSWORD` (default from `application.properties`)
- `PORT` (default: `8080`)
- `JWT_SECRET` (default: `change-me`)
- `JWT_EXPIRATION_MS` (default: `86400000`)

Copy and edit:

```bash
cp .env.example .env
```

## Local Setup

1. Install JDK 21 and Maven (or use Maven Wrapper).
2. Start MySQL and create a `postify` database.
3. Configure `.env` with DB credentials.
4. Run the app:

```bash
mvn spring-boot:run
```

Windows Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Open: `http://localhost:8080`

## Testing

The project contains:

- Web/API tests for controllers using `@WebMvcTest` + `MockMvc`
- Integration tests for services using `@SpringBootTest` + H2 in-memory DB (`test` profile)

Test config file:

- `src/test/resources/application-test.properties`

Run tests:

```bash
mvn test
```

Windows Maven wrapper:

```powershell
.\mvnw.cmd test
```

## Important Routes

- `GET /` home feed
- `GET /login`
- `GET /register`
- `POST /register/add`
- `GET /profile/{username}`
- `GET /profile/edit`
- `POST /profile/edit`
- `POST /posts/create`
- `POST /posts/delete/{postId}`
- `GET /posts/{postId}/comments`
- `POST /posts/{postId}/comments`
- `POST /posts/{postId}/like`
- `POST /follow/{username}`
- `GET /search?q={query}`
- `GET /notifications/stream` (SSE)
- `GET /admin/dashboard`
- `GET /admin/users`
- `POST /admin/users/promote/{id}`
- `POST /admin/users/delete/{id}`
- `GET /admin/logs`

## File Uploads

- Upload directory: `uploads/` (git-ignored runtime data)
- Avatar and post images are persisted on disk and exposed through configured web paths

## Project Structure

```text
src/
  main/
    java/com/omar/postify/
      config/
      controller/
      dto/
      entities/
      exception/
      repository/
      security/
      service/
      util/
    resources/
      templates/
      static/
      application.properties
  test/
    java/com/omar/postify/
      controller/
      integration/
    resources/
      application-test.properties
```

## Contact

`3mur1111@gmail.com`
