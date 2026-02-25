package com.rateit.rateit_backend.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.rateit.rateit_backend.service.PostService;
import com.rateit.rateit_backend.dto.request.CreatePostRequest;
import com.rateit.rateit_backend.dto.response.PostResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/video")
    public ResponseEntity<PostResponse> uploadVideo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        PostResponse response = postService.uploadVideo(id, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/video")
    public ResponseEntity<StreamingResponseBody> streamVideo(@PathVariable Long id,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {
        Resource video = postService.streamVideo(id);
        long fileSize = video.contentLength();

        long start = 0;
        long end = fileSize - 1;

        if (rangeHeader != null) {
            String range = rangeHeader.replace("bytes=", "");
            String[] parts = range.split("-");
            start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
        }

        if (start > end || end >= fileSize) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header("Content-Range", "bytes */" + fileSize)
                    .build();
        }

        final long rangeStart = start;
        final long rangeEnd = end;
        final long contentLength = rangeEnd - rangeStart + 1;

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = video.getInputStream()) {
                inputStream.skip(rangeStart);
                byte[] buffer = new byte[8192];
                long bytesToRead = contentLength;
                int bytesRead;
                while (bytesToRead > 0 && (bytesRead = inputStream.read(buffer, 0,
                        (int) Math.min(buffer.length, bytesToRead))) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesToRead -= bytesRead;
                }
            }
        };

        HttpStatus status = (rangeHeader != null) ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;

        return ResponseEntity.status(status)
                .header("Content-Type", "video/mp4")
                .header("Accept-Ranges", "bytes")
                .header("Content-Length", String.valueOf(contentLength))
                .header("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileSize)
                .body(body);
    }

    @PostMapping("/{id}/views")
    public ResponseEntity<Void> trackView(@PathVariable Long id) {
        postService.trackView(id);
        return ResponseEntity.noContent().build();
    }

}
