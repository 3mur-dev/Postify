# Postify

Modern, minimal social media feed built with Spring Boot + Thymeleaf. Users can register, log in, post text or images, like, follow, search people, edit profiles, and browse a responsive feed with infinite scroll capabilities.

## Features
- **Authentication**: Email/username login with password hashing (BCrypt)
- **Posts**: Create posts with optional image attachments; delete your own posts
- **Engagement**: Like/unlike posts and see live counts
- **Connections**: Follow/unfollow users; profile pages with follower/following stats
- **Profile Management**: Edit profiles with avatar upload
- **Discovery**: Search users by username
- **UI/UX**: Responsive design with mobile-friendly navbar and load-more feed

## Tech Stack
- **Backend**: Java 21, Spring Boot 4 (Web, Security, Data JPA, Validation)
- **Frontend**: Thymeleaf + Tailwind CDN
- **Database**: MySQL + JPA/Hibernate
- **Security**: BCrypt password hashing, JWT utility ready for API use

## Quick Start (Local)

### Prerequisites
- JDK 21
- Maven 3.8+
- MySQL 8.0+

### Setup Steps
1. Clone the repository
   ```bash
   git clone https://github.com/3mur-dev/Postify.git
   cd Postify
   ```

2. Create database
   ```sql
   CREATE DATABASE postify;
   ```

3. Configure environment
   ```bash
   cp .env.example .env
   ```
   
   Edit `.env`:
   ```properties
   DB_URL=jdbc:mysql://localhost:3306/postify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   DB_USERNAME=root
   DB_PASSWORD=yourpassword
   PORT=8080
   JWT_SECRET=your-secret-key-at-least-32-chars
   JWT_EXPIRATION_MS=86400000
   ```

4. Run the application
   ```bash
   mvn spring-boot:run
   ```
   
   Visit: `http://localhost:8080`

5. Create test account
   - Register with email/username
   - Start posting and following users

## Project Structure
```
src/main/java/com/omar/postify/
├── controller/          # HTTP request handlers
├── service/             # Business logic (posts, follows, likes)
├── repository/          # Database access layer (JPA)
├── entity/              # Domain models (User, Post, Follow, Like)
└── config/              # Spring Security, file upload config

src/main/resources/
├── templates/           # Thymeleaf HTML views
└── static/              # CSS, JS, default avatar image

uploads/
├── avatars/             # User profile pictures
└── posts/               # Post images
```

## File Uploads
- **Avatars**: Stored in `uploads/avatars/`, served at `/images/avatars/...`
- **Post images**: Stored in `uploads/posts/`, served at `/images/posts/...`
- Max file size: 5MB per image

## Running Tests
```bash
# Unit and integration tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## Deployment

### Docker (Recommended)
Create a `docker-compose.yml`:
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: postify
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  app:
    build: .
    depends_on:
      - mysql
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/postify
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root

volumes:
  mysql_data:
```

Run:
```bash
docker-compose up --build
```

## Security Considerations
- Passwords encrypted with BCrypt (10 rounds)
- Session-based authentication with Thymeleaf form protection
- File upload validation (whitelist image types)
- CSRF tokens on all forms
- SQL injection protected via Hibernate parameterized queries

## Future Improvements
- [ ] Infinite scroll for feed (cursor-based pagination)
- [ ] Comments and nested replies
- [ ] Direct messaging between users
- [ ] Notifications system (real-time with WebSockets)
- [ ] Rate limiting per endpoint
- [ ] GitHub Actions CI/CD workflow
- [ ] API versioning (REST API alongside MVC)
- [ ] Email verification on registration

## Known Limitations
- File uploads stored locally (not cloud storage)
- No real-time notifications yet
- Single instance deployment (limited horizontal scaling)

## License
MIT License - see LICENSE file for details

## Contributing
Feel free to fork, submit issues, or create pull requests.

---

**Project Status**: MVP (Minimum Viable Product) - Core social features working, ready for expansion