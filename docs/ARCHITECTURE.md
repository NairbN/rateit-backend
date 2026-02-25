# RateIt Backend — Architecture Context

## What is RateIt?

A TikTok-style short video platform backend. Users can upload short videos, stream them, and see how many views they've received. Built as a learning project to practice production-grade Spring Boot patterns: layered architecture, Docker, Testcontainers, and eventually Kubernetes + cloud deployment.

**Project goal:** Build a complete, well-tested backend — then use it as a base to learn K8s, AWS (RDS, S3, EKS), microservices, and event-driven architecture (Kafka).

**No auth for now.** Spring Security + JWT is planned after the MVP feature set is complete.

---

## How to Pick Up Where You Left Off

1. Check the **Progress Log** at the bottom of this file — completed items are `[x]`, next up is the first `[ ]`
2. Read **`docs/FEATURE_GUIDE.md`** — contains hints and implementation notes for each remaining feature
3. Run `./mvnw.cmd test` to confirm all tests still pass before starting
4. The full stack runs via `docker compose up --build`

**Mentoring style in use:** step-by-step, bottom-up (entity → repository → service → controller → tests). Don't skip layers.

---

## Tech Stack

| Layer            | Technology                  |
| ---------------- | --------------------------- |
| Language         | Java 17                     |
| Framework        | Spring Boot 4.x             |
| Build            | Maven                       |
| ORM              | Spring Data JPA + Hibernate |
| DB Migrations    | Flyway                      |
| Database         | PostgreSQL 16               |
| Containerization | Docker + Docker Compose     |

---

## MVP Features

1. Create post (video metadata)
2. Upload video file (local storage for now)
3. Mark upload complete
4. Stream video with HTTP Range support
5. Track view events
6. Return analytics (view count)
7. Feed endpoint

**Non-goals (for now):** auth, cloud storage (S3/GCS), microservices, frontend.

---

## Environment Rules

- The **entire application runs in Docker** — Spring Boot + PostgreSQL both in containers.
- Containers communicate via Docker network using service names.
- DB hostname inside Docker: `db` (not `localhost`).
- Correct datasource URL for Docker: `jdbc:postgresql://db:5432/rateit_db`
- `localhost:5432` is only valid when running Spring Boot on the host directly (dev shortcut).
- `docker compose up --build` is the canonical way to run the full stack.

---

## Package Structure

```
src/main/java/com/rateit/rateit_backend/
├── RateitBackendApplication.java   ← entry point
├── config/          @Configuration beans — storage paths, web MVC settings
├── controller/      HTTP layer only — parse request, delegate to service, return DTO
├── service/         ALL business logic lives here — controllers never touch repositories
├── repository/      Spring Data JPA interfaces — data access only
├── entity/          @Entity classes — mirror the DB schema
│   └── enums/       Domain enums (e.g. PostStatus) — state that belongs to the domain model
├── dto/
│   ├── request/     Inbound payloads — validated with @Valid
│   └── response/    Outbound responses — never expose entities directly
├── storage/         File I/O abstraction — local disk now, swap to S3 later without touching services
└── exception/       Custom exceptions + @ControllerAdvice global error handler
```

### Why These Layers Exist

The database speaks SQL. The application speaks Java. Each layer is a translator and gatekeeper between those worlds.

| Layer          | Responsibility                                                  | Speaks               |
| -------------- | --------------------------------------------------------------- | -------------------- |
| **Entity**     | Mirror of a DB table row — no business logic, no HTTP awareness | JPA / DB schema      |
| **Repository** | Gateway to the DB — the rest of the app never writes SQL        | JPA queries          |
| **Service**    | All business rules live here — validates, orchestrates, decides | Java / domain logic  |
| **DTO**        | Shape of data crossing the API boundary — decoupled from entity | JSON / HTTP contract |
| **Controller** | Handles HTTP — routes requests to services, returns responses   | HTTP                 |

Each layer only talks to the one directly next to it:

```
HTTP Request → Controller → Service → Repository → Database
```

**Why this matters:**

- Change the DB schema? Update entity + repository. Controller doesn't care.
- Change the API response shape? Update the DTO. Entity doesn't change.
- Change a business rule? Update the service. Nothing else moves.
- Want to unit test business logic? Mock the repository, test the service in pure Java — no HTTP, no DB needed.

### Layer Rules (enforced by discipline, not framework)

- Controller → Service → Repository (never skip a layer)
- Entities never leave the service layer (map to DTOs before returning)
- Services own transactions (`@Transactional`)
- Controllers are thin — no `if` logic, no data transformation beyond calling service methods

---

## Database Schema

### posts (V1\_\_init.sql)

| Column     | Type         | Notes                          |
| ---------- | ------------ | ------------------------------ |
| id         | bigserial    | PK                             |
| caption    | varchar(300) | Optional                       |
| status     | varchar(30)  | NOT NULL — e.g. PENDING, READY |
| video_key  | varchar(255) | Path/key to video file         |
| created_at | timestamptz  | Defaults to now()              |

---

## Docker Setup (target)

```
docker-compose.yml
├── db      → postgres:16, port 5432, healthcheck via pg_isready
└── api     → Spring Boot app, built from Dockerfile, depends_on db healthy
```

### Dockerfile strategy: multi-stage build

- **Stage 1 (build):** `maven:3.9-eclipse-temurin-17` — runs `mvn package -DskipTests`
- **Stage 2 (runtime):** `eclipse-temurin:17-jre-alpine` — copies JAR only, minimal image

Why: The final image contains no Maven, no source code, no build tools — only the JRE and the JAR.

### Config override for Docker

Spring Boot environment variables override `application.yaml` automatically.
`docker-compose.yml` sets:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/rateit_db
```

This avoids needing a separate `application-docker.yaml`.

---

## Test Strategy

Three levels of tests. All run with `./mvnw.cmd test`.

| Type        | Annotation                            | What it tests                                     | DB?                  |
| ----------- | ------------------------------------- | ------------------------------------------------- | -------------------- |
| Unit        | `@ExtendWith(MockitoExtension.class)` | Service logic only — everything else mocked       | No                   |
| Slice       | `@WebMvcTest`                         | HTTP layer only — service mocked, no real logic   | No                   |
| Integration | `@SpringBootTest` + `@Testcontainers` | Full stack — real PostgreSQL via Docker container | Yes (Testcontainers) |

**Test file naming convention:**

- `*Test.java` — unit or slice test (picked up by Maven Surefire)
- `*IntegrationTest.java` — full-stack integration test (also Surefire)
- `*IT.java` — NOT picked up by Surefire by default (requires Failsafe plugin). Avoid this suffix.

**Current test inventory:**

| File                            | Type        | What it covers                                                                    |
| ------------------------------- | ----------- | --------------------------------------------------------------------------------- |
| `RateitBackendApplicationTests` | Integration | Spring context loads, Flyway migrations run                                       |
| `PostServiceTest`               | Unit        | `createPost`, `uploadVideo`, `streamVideo` service logic                          |
| `PostControllerTest`            | Slice       | `POST /posts` — 201/400; `POST /posts/{id}/video` — 200; `GET /posts/{id}/video` — 206 |
| `CreatePostIntegrationTest`     | Integration | Full create post flow, real DB                                                    |
| `UploadVideoIntegrationTest`    | Integration | Create post + upload video, real DB + real file system                            |

---

## Spring Boot 4.x Gotchas

This project uses **Spring Boot 4.0.x** which is a major version with breaking changes from 3.x. Key differences:

**Test annotation packages moved:**

```java
// Spring Boot 3.x (WRONG for this project):
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

// Spring Boot 4.x (CORRECT):
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
```

**`@MockBean` replaced by `@MockitoBean`:**

```java
// Spring Boot 3.x (WRONG):
@MockBean private PostService postService;

// Spring Boot 4.x (CORRECT):
@MockitoBean private PostService postService;
```

**`ObjectMapper` not auto-injectable in `@WebMvcTest`:**
Instantiate manually:

```java
private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
```

`findAndRegisterModules()` is required for `Instant` serialization (registers `JavaTimeModule`).

**Test dependencies needed (beyond `spring-boot-starter-test`):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

This provides `@WebMvcTest` and `@AutoConfigureMockMvc` in the 4.x package location.

---

## Key Design Decisions

| Decision                            | Reasoning                                                                                                                                    |
| ----------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `ddl-auto: validate` (not `update`) | Flyway owns the schema. Hibernate should only validate it matches, never alter it. Both managing schema causes conflicts.                    |
| DTOs separate from entities         | Entities are internal DB representations. DTOs control the API contract. Decoupling means you can change DB schema without breaking the API. |
| `storage/` as its own package       | Abstracts file I/O so the service layer doesn't care if storage is local disk or S3. Swap the implementation, not the interface.             |
| No auth in MVP                      | Keeps the MVP scope tight. Auth will be added as a separate layer later (Spring Security + JWT).                                             |
| Flyway for migrations               | Versioned, repeatable, tracked in git. Never rely on Hibernate auto-DDL in any environment beyond early local dev.                           |

---

## Progress Log

- [x] Project scaffold initialized (Spring Boot 4.x, Maven)
- [x] PostgreSQL running in Docker
- [x] Flyway V1 migration — `posts` table created
- [x] Package structure created (including entity/enums subpackage)
- [x] Dockerfile (multi-stage build)
- [x] docker-compose updated with `api` service + healthcheck
- [x] application.yaml — ddl-auto: validate, Docker URL comment added
- [x] Smoke test passed — `docker compose up --build` → health UP
- [x] PostStatus enum (entity/enums/)
- [x] Post entity
- [x] Post repository
- [x] DTOs (CreatePostRequest, PostResponse)
- [x] Post service
- [x] PostServiceTest (unit test — Mockito)
- [x] Post controller (POST /posts)
- [x] PostControllerTest (@WebMvcTest slice test — MockMvc + @MockitoBean)
- [x] CreatePostIntegrationTest (full-stack integration test — Testcontainers + real PostgreSQL)
- [x] Video upload (multipart) — POST /posts/{id}/video, LocalVideoStorage, StorageConfig, GlobalExceptionHandler
- [x] Video streaming (HTTP Range) — GET /posts/{id}/video, Range header, 206 Partial Content, StreamingResponseBody
- [ ] View tracking
- [ ] Analytics endpoint
- [ ] Feed endpoint
