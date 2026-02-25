package com.rateit.rateit_backend.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.rateit.rateit_backend.dto.response.PostResponse;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

import com.rateit.rateit_backend.dto.request.CreatePostRequest;
import com.rateit.rateit_backend.entity.Post;
import com.rateit.rateit_backend.entity.ViewEvent;
import com.rateit.rateit_backend.entity.enums.PostStatus;
import com.rateit.rateit_backend.exception.PostNotFoundException;
import com.rateit.rateit_backend.repository.PostRepository;
import com.rateit.rateit_backend.repository.ViewEventRepository;
import com.rateit.rateit_backend.storage.VideoStorage;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final VideoStorage videoStorage;
    private final ViewEventRepository viewEventRepository;

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        // Create and save the post entity
        Post post = new Post();
        post.setCaption(request.getCaption());
        post.setStatus(PostStatus.PENDING_UPLOAD);

        Post saved = postRepository.save(post);

        // Convert to response DTO
        return new PostResponse(
                saved.getId(),
                saved.getCaption(),
                saved.getStatus().name(),
                saved.getCreatedAt());
    }

    @Transactional
    public PostResponse uploadVideo(Long id, MultipartFile file) {
        // Find the post by ID
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));

        // Simulate video upload and update post status
        // In a real application, you would handle file storage here
        String videoKey = videoStorage.store(id, file);
        post.setVideoKey(videoKey);
        post.setStatus(PostStatus.READY);
        Post saved = postRepository.save(post);

        // Convert to response DTO
        return new PostResponse(
                saved.getId(),
                saved.getCaption(),
                saved.getStatus().name(),
                saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Resource streamVideo(Long id) {
        // Find the post by ID
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id));

        // Check if the post is ready for streaming
        if (post.getStatus() != PostStatus.READY) {
            throw new RuntimeException("Post is not ready for streaming");
        }

        if (post.getVideoKey() == null) {
            throw new RuntimeException("No video associated with this post: " + id);
        }
        return videoStorage.load(post.getVideoKey());
    }

    @Transactional
    public void trackView(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        if (post.getStatus() != PostStatus.READY) {
            throw new RuntimeException("Post is not ready for viewing");
        }

        ViewEvent event = new ViewEvent();
        event.setPostId(postId);
        viewEventRepository.save(event);
    }

}
