# Postify

Modern, minimal social feed built with Spring Boot + Thymeleaf. Users can register, log in, post text or images, like, follow, search people, edit profiles, and browse a responsive feed with “load more” pagination.

## Features
- Email/username login with password hashing (BCrypt)
- Create posts with optional image attachments; delete your own posts
- Like/unlike posts and see live counts
- Follow/unfollow users; profile pages with follower/following stats
- Profile editing with avatar upload
- Search users by username
- Responsive UI with mobile-friendly navbar and load-more feed

## Tech Stack
- Java 21, Spring Boot 4 (Web, Security, Data JPA, Validation)
- Thymeleaf + Tailwind CDN
- MySQL + JPA/Hibernate
- JWT utility ready for API use

## Quick Start (Local)
1. Install JDK 21, Maven, and MySQL.
2. Create a database `postify` (or adjust `DB_URL`).
3. Copy `.env.example` to `.env` and set:
   ```
   DB_URL=jdbc:mysql://localhost:3306/postify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   DB_USERNAME=youruser
   DB_PASSWORD=yourpass
   PORT=8080
   JWT_SECRET=change-me
   JWT_EXPIRATION_MS=86400000
   ```
4. Run: `mvn spring-boot:run`
5. Visit: `http://localhost:8080`

## File Uploads
- Avatars: stored in `uploads/avatars`, served at `/images/avatars/...`
- Post images: stored in `uploads/posts`, served at `/images/posts/...`

## Running Tests
- Unit/integration tests: `mvn test`

## Project Structure
- `src/main/java/com/omar/postify` — Spring MVC controllers, services, repositories, entities
- `src/main/resources/templates` — Thymeleaf views
- `src/main/resources/static` — static assets (default avatar)
- `uploads/` — runtime-uploaded files (git-ignored)

## Roadmap Ideas
- Infinite scroll for feed (cursor-based)
- Comments and notifications
- Rate limiting and CSRF tokens enabled with Thymeleaf forms
- CI workflow (GitHub Actions) running `mvn test` on push

## License
MIT (add a LICENSE file if you want to formalize it)


Email: 3mur1111@gmail.com
