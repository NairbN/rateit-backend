# RateIt Backend — Feature Implementation Guide

A self-guided reference for the remaining MVP features. Each section describes what to build, the key concepts you'll need, and what the test coverage should look like.

---

## Feature 2: Video Upload

**Endpoint:** `POST /posts/{id}/video`
**What it does:** Accepts a video file, saves it to disk, updates the post's status to `READY` and stores the file path in `video_key`.

### What you need to know

**Multipart file upload**
Spring represents uploaded files as `MultipartFile`. Your controller method will look like:
```java
@PostMapping("/{id}/video")
public ResponseEntity<PostResponse> uploadVideo(
    @PathVariable Long id,
    @RequestParam("file") MultipartFile file) { ... }
```

**The `storage/` package**
This is where file I/O lives. Create an interface so the service doesn't care about the implementation:
```
storage/
  VideoStorage.java         ← interface: String store(Long postId, MultipartFile file)
  LocalVideoStorage.java    ← implementation: writes to disk
```
The service calls `videoStorage.store(...)` — it doesn't know or care if storage is local disk or S3.

**Configuration**
You need to configure where files are stored. Add to `application.yaml`:
```yaml
app:
  storage:
    location: ./uploads
```
Read it with `@Value("${app.storage.location}")` in your `LocalVideoStorage`.

Also add multipart size limits to `application.yaml`:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB
```

**Post lifecycle**
The post moves from `PENDING_UPLOAD` → `READY` after a successful upload.
The service needs to:
1. Look up the post by ID (throw 404 if not found)
2. Validate it's in `PENDING_UPLOAD` state
3. Save the file via `VideoStorage`
4. Update `post.setVideoKey(...)` and `post.setStatus(PostStatus.READY)`
5. Return the updated `PostResponse`

**Custom exception**
Create `exception/PostNotFoundException.java` and a `@ControllerAdvice` global handler that maps it to `404 Not Found`.

**Docker volume**
The `uploads/` directory is already volume-mounted in `docker-compose.yml` (`./uploads:/app/uploads`). Make sure the storage path in `application.yaml` matches `/app/uploads` when running in Docker (use an env var override in docker-compose, same pattern as the DB URL).

### Tests to write
- `PostServiceTest` — add test for `uploadVideo`: mock repository + mock VideoStorage, verify status becomes READY
- `PostControllerTest` — add test for `POST /{id}/video` with `MockMultipartFile`
- `UploadVideoIntegrationTest` — full stack: create a post, upload a file, assert 200 and status READY

---

## Feature 3: Video Streaming

**Endpoint:** `GET /posts/{id}/video`
**What it does:** Streams the video file to the client. Supports HTTP Range requests so clients can seek (jump to a timestamp) without re-downloading the whole file.

### What you need to know

**HTTP Range requests**
Clients send a `Range` header: `Range: bytes=0-1048575`
Your response should be `206 Partial Content` with:
- `Content-Range: bytes 0-1048575/10485760` (range served / total size)
- `Accept-Ranges: bytes`
- `Content-Type: video/mp4`

If no Range header is sent, return the full file with `200 OK`.
If the range is invalid, return `416 Range Not Satisfiable`.

**Don't load the whole file into memory**
Use `StreamingResponseBody` — Spring streams the response lazily:
```java
StreamingResponseBody body = outputStream -> {
    // write bytes from file into outputStream
};
return ResponseEntity.status(206).body(body);
```

**Key file I/O**
You'll use `RandomAccessFile` or `FileChannel` to seek to the start byte and read only the requested range.

**Add to `VideoStorage` interface**
Add a method to load a file by its key: `Resource load(String videoKey)`
`LocalVideoStorage` returns a `FileSystemResource`.

### Tests to write
- `PostControllerTest` — add test for `GET /{id}/video` with a `Range` header, assert `206`
- Streaming integration tests are harder to write — focus on the unit + slice tests here

---

## Feature 4: View Tracking

**Endpoint:** `POST /posts/{id}/views`
**What it does:** Records that a user watched a video. Returns `204 No Content`.

### What you need to know

**New Flyway migration**
Create `V2__add_view_events.sql`:
```sql
CREATE TABLE view_events (
    id        bigserial PRIMARY KEY,
    post_id   bigint NOT NULL REFERENCES posts(id),
    viewed_at timestamptz NOT NULL DEFAULT now()
);
```

**New entity + repository**
`ViewEvent` entity with `postId` and `viewedAt`.
`ViewEventRepository extends JpaRepository<ViewEvent, Long>`.

**Service logic**
1. Verify the post exists and is in `READY` state (can't view an unuploaded video)
2. Save a new `ViewEvent`
3. Return nothing (void) — controller returns `204`

**Controller**
```java
@PostMapping("/{id}/views")
public ResponseEntity<Void> trackView(@PathVariable Long id) {
    postService.trackView(id);
    return ResponseEntity.noContent().build();
}
```

### Tests to write
- `PostServiceTest` — test `trackView`: verify `ViewEventRepository.save()` is called
- `PostControllerTest` — test `POST /{id}/views` returns `204`
- Integration test — create post, upload video, POST /views, assert 204

---

## Feature 5: Analytics Endpoint

**Endpoint:** `GET /posts/{id}/analytics`
**What it does:** Returns the view count for a post.

### What you need to know

**New response DTO**
Create `dto/response/AnalyticsResponse.java`:
```java
private Long postId;
private Long viewCount;
```

**Custom repository method**
`ViewEventRepository` needs to count views for a post:
```java
long countByPostId(Long postId);
```
Spring Data JPA derives the query from the method name — no SQL needed.

**This is where `@DataJpaTest` pays off**
Write `ViewEventRepositoryTest` with `@DataJpaTest` + Testcontainers to verify `countByPostId` returns the correct count.

**Service logic**
1. Verify post exists
2. Call `viewEventRepository.countByPostId(id)`
3. Return `AnalyticsResponse`

### Tests to write
- `ViewEventRepositoryTest` — `@DataJpaTest`, insert rows, assert count (your first repository test!)
- `PostControllerTest` — test `GET /{id}/analytics` returns correct JSON shape
- Integration test — create post, record views, assert analytics count matches

---

## Feature 6: Feed Endpoint

**Endpoint:** `GET /posts/feed`
**What it does:** Returns a paginated list of `READY` posts, newest first.

### What you need to know

**Custom repository query**
```java
Page<Post> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);
```
Spring Data derives this from the method name. `Page<T>` wraps the results with pagination metadata.

**Pagination via query params**
Spring MVC can bind `Pageable` automatically:
```java
@GetMapping("/feed")
public ResponseEntity<Page<PostResponse>> getFeed(Pageable pageable) { ... }
```
Client calls: `GET /posts/feed?page=0&size=10`

**Response DTO**
Map each `Post` entity to `PostResponse` inside the service. Return `Page<PostResponse>`.

**This is also a good `@DataJpaTest` candidate**
Insert posts with different statuses, assert only READY ones appear in the feed, and in the right order.

### Tests to write
- `PostRepositoryTest` — `@DataJpaTest`, insert READY + PENDING posts, assert feed only returns READY in correct order
- `PostControllerTest` — test `GET /posts/feed` with mocked service
- Integration test — create multiple posts, upload some, assert feed returns only READY ones

---

## General Patterns to Follow

- **Bottom-up:** migration → entity → repository → service → controller
- **Test as you go:** write the service test before moving to the controller
- **Entities never leave the service layer** — always map to DTOs before returning
- **Custom exceptions** map to HTTP status codes in `@ControllerAdvice`
- **New DB columns = new Flyway migration** — never edit existing migrations
