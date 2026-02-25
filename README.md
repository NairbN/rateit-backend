# RateIt Backend

A TikTok-style short video platform backend built with Spring Boot 4.x. This is a learning project exploring REST API design, Docker, Testcontainers, and cloud-native patterns.

## What it does

RateIt lets users upload short videos, stream them with seek support, and track views. The backend exposes a REST API consumed by a (future) mobile/web frontend.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 4.x |
| Database | PostgreSQL (via Docker) |
| Migrations | Flyway |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers |

## Getting Started

### Prerequisites
- Docker Desktop running
- Java 21+
- Maven

### Run with Docker

```bash
docker compose up --build
```

API will be available at `http://localhost:8080`.

### Run tests

```bash
./mvnw test
```

Docker must be running for integration tests (Testcontainers spins up a real PostgreSQL container).

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/posts` | Create a new post |
| `POST` | `/posts/{id}/video` | Upload a video file |
| `GET` | `/posts/{id}/video` | Stream a video (supports HTTP Range) |
| `POST` | `/posts/{id}/views` | Record a view event |
| `GET` | `/posts/{id}/analytics` | Get view count *(coming soon)* |
| `GET` | `/posts/feed` | Paginated feed of ready posts *(coming soon)* |

## Project Structure

```
src/
  main/
    java/com/rateit/rateit_backend/
      config/        # Spring @Configuration classes (StorageConfig)
      controller/    # REST controllers
      dto/           # Request/response DTOs
      entity/        # JPA entities
      exception/     # Custom exceptions + GlobalExceptionHandler
      repository/    # Spring Data JPA repositories
      service/       # Business logic
      storage/       # VideoStorage interface + LocalVideoStorage
    resources/
      db/migration/  # Flyway SQL migrations
      application.yaml
  test/              # Unit, slice, and integration tests
docs/
  ARCHITECTURE.md    # Design decisions, test strategy, Spring Boot 4.x gotchas
  FEATURE_GUIDE.md   # Implementation hints for remaining features
```

## Documentation

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — architecture decisions, test strategy, known gotchas
- [FEATURE_GUIDE.md](docs/FEATURE_GUIDE.md) — self-guided hints for implementing remaining features
