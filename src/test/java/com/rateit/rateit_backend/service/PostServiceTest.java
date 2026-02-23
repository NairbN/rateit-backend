package com.rateit.rateit_backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.rateit.rateit_backend.entity.Post;
import com.rateit.rateit_backend.entity.enums.PostStatus;
import com.rateit.rateit_backend.dto.request.CreatePostRequest;
import com.rateit.rateit_backend.dto.response.PostResponse;

import com.rateit.rateit_backend.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void createPost_shouldReturnResponseWithPendingStatus() {
        // Given
        CreatePostRequest request = new CreatePostRequest();
        request.setCaption("Test caption");

        Post savedPost = new Post();
        savedPost.setId(1L);
        savedPost.setCaption(request.getCaption());
        savedPost.setStatus(PostStatus.PENDING_UPLOAD);
        savedPost.setCreatedAt(Instant.now());

        when(postRepository.save(any(Post.class))).thenReturn(savedPost);

        // When
        PostResponse response = postService.createPost(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCaption()).isEqualTo("Test caption");
        assertThat(response.getStatus()).isEqualTo("PENDING_UPLOAD");
        assertThat(response.getCreatedAt()).isNotNull();
        verify(postRepository).save(any(Post.class));
    }

}
