package com.rateit.rateit_backend;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class TrackViewIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void overrideStorage(DynamicPropertyRegistry registry) {
        registry.add("app.storage.location", tempDir::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void trackView_shouldReturn204AfterUpload() throws Exception {
        // Step 1: Create a post
        String createJson = mockMvc.perform(post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"caption\":\"Test caption\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long postId = objectMapper.readTree(createJson).get("id").asLong();

        // Step 2: Upload video for the post
        MockMultipartFile videoFile = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                "dummy video content".getBytes());

        mockMvc.perform(multipart("/posts/" + postId + "/video")
                .file(videoFile))
                .andExpect(status().isOk());

        // Step 3: Track view and expect 204 No Content
        mockMvc.perform(post("/posts/" + postId + "/views"))
                .andExpect(status().isNoContent());
    }
}
