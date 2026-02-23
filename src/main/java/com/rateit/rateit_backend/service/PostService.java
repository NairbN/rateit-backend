package com.rateit.rateit_backend.service;

import org.springframework.stereotype.Service;

import com.rateit.rateit_backend.dto.response.PostResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.rateit.rateit_backend.dto.request.CreatePostRequest;
import com.rateit.rateit_backend.entity.Post;
import com.rateit.rateit_backend.entity.enums.PostStatus;
import com.rateit.rateit_backend.repository.PostRepository;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

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
}
