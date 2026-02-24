package com.rateit.rateit_backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.rateit.rateit_backend.service.PostService;
import com.rateit.rateit_backend.dto.request.CreatePostRequest;
import com.rateit.rateit_backend.dto.response.PostResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(PostController.class)
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void createPost_shouldReturn201WithResponse() throws Exception {
        // Given
        CreatePostRequest request = new CreatePostRequest();
        request.setCaption("Test caption");

        PostResponse response = new PostResponse(1L, "Test caption", "PENDING_UPLOAD", Instant.now());
        when(postService.createPost(any(CreatePostRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.caption").value("Test caption"))
                .andExpect(jsonPath("$.status").value("PENDING_UPLOAD"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createPost_shouldReturn400WhenCaptionIsBlank() throws Exception {
        // Given
        CreatePostRequest request = new CreatePostRequest();
        request.setCaption(""); // Blank caption

        // When & Then
        mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadVideo_shouldReturn200WithReadyStatus() throws Exception {
        // Given
        Long postId = 1L;
        PostResponse response = new PostResponse(postId, "Test caption", "READY", Instant.now());
        when(postService.uploadVideo(eq(1L), any(MultipartFile.class))).thenReturn(response);

        MockMultipartFile mockfile = new MockMultipartFile(
                "file",
                "video.mp4",
                "video/mp4",
                "dummy video content".getBytes());

        // When & Then
        mockMvc.perform(multipart("/posts/1/video").file(mockfile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.caption").value("Test caption"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.createdAt").exists());
    }
}
