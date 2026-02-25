package com.rateit.rateit_backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.eq;
import java.time.Instant;
import java.util.Optional;
import com.rateit.rateit_backend.entity.Post;
import com.rateit.rateit_backend.entity.ViewEvent;
import com.rateit.rateit_backend.entity.enums.PostStatus;
import com.rateit.rateit_backend.dto.request.CreatePostRequest;
import com.rateit.rateit_backend.dto.response.PostResponse;
import com.rateit.rateit_backend.storage.VideoStorage;
import com.rateit.rateit_backend.repository.PostRepository;
import com.rateit.rateit_backend.repository.ViewEventRepository;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private ViewEventRepository viewEventRepository;

    @Mock
    private VideoStorage videoStorage;

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

    @Test
    void uploadVideo_shouldReturnResponseWithReadyStatus() {
        // Given
        Long postId = 1L;
        MultipartFile mockFile = mock(MultipartFile.class);

        Post existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setStatus(PostStatus.PENDING_UPLOAD);
        existingPost.setCreatedAt(Instant.now());
        existingPost.setCaption("Test caption");

        Post savedPost = new Post();
        savedPost.setId(postId);
        savedPost.setStatus(PostStatus.READY);
        savedPost.setCreatedAt(existingPost.getCreatedAt());
        savedPost.setCaption(existingPost.getCaption());
        savedPost.setVideoKey("uploads/1/video.mp4");

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(videoStorage.store(eq(postId), any(MultipartFile.class))).thenReturn("uploads/1/video.mp4");

        // When
        PostResponse response = postService.uploadVideo(postId, mockFile);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(postId);
        assertThat(response.getCaption()).isEqualTo("Test caption");
        assertThat(response.getStatus()).isEqualTo("READY");
        assertThat(response.getCreatedAt()).isNotNull();
        verify(videoStorage).store(eq(postId), any(MultipartFile.class));
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void streamVideo_shouldReturnResource() {
        // Given
        Long postId = 1L;
        String videoKey = "uploads/1/video.mp4";

        Post existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setStatus(PostStatus.READY);
        existingPost.setCreatedAt(Instant.now());
        existingPost.setCaption("Test caption");
        existingPost.setVideoKey(videoKey);

        Resource mockResource = new ByteArrayResource(new byte[] { 1, 2, 3 });

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(videoStorage.load(videoKey)).thenReturn(mockResource);

        // When
        Resource resource = postService.streamVideo(postId);

        // Then
        assertThat(resource).isNotNull();
        assertThat(resource).isEqualTo(mockResource);
        verify(videoStorage).load(videoKey);
    }

    @Test
    void trackView_shouldSaveViewEvent() {
        Long postId = 1L;
        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.READY);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        postService.trackView(postId);
        verify(postRepository).findById(postId);
        verify(viewEventRepository).save(any(ViewEvent.class));
    }

}
