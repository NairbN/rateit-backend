package com.rateit.rateit_backend.storage;

import java.nio.file.Files;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalVideoStorage implements VideoStorage {

    private final String storageLocation;

    @Override
    public String store(Long postId, MultipartFile file) {
        try {
            Path uploadDir = Paths.get(storageLocation, String.valueOf(postId));
            Files.createDirectories(uploadDir);

            String filename = file.getOriginalFilename();
            Path destination = uploadDir.resolve(filename);
            file.transferTo(destination);
            return destination.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to store video for post ID: " + postId, e);
        }
    }

    @Override
    public Resource load(String videoKey) {
        Resource resource = new FileSystemResource(videoKey);
        if (!resource.exists()) {
            throw new RuntimeException("Video not found: " + videoKey);
        }
        return resource;
    }

}
