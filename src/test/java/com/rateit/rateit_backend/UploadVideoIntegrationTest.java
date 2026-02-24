package com.rateit.rateit_backend;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rateit.rateit_backend.dto.request.CreatePostRequest;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UploadVideoIntegrationTest {

    @TempDir
    static Path tempDir;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void overrideStorageLocation(DynamicPropertyRegistry registry) {
        registry.add("app.storage.location", () -> tempDir.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void uploadVideo_shouldPersistAndReturnReady() throws Exception {
        // Step 1: Create a post
        CreatePostRequest createRequest = new CreatePostRequest();
        createRequest.setCaption("Test caption");

        String createResponse = mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long postId = objectMapper.readTree(createResponse).get("id").asLong();

        // Step 2: Upload video for the created post
        MockMultipartFile mockfile = new MockMultipartFile(
                "file",
                "video.mp4",
                "video/mp4",
                "dummy video content".getBytes());

        mockMvc.perform(multipart("/posts/{id}/video", postId).file(mockfile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.caption").value("Test caption"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.createdAt").exists());
    }
}
