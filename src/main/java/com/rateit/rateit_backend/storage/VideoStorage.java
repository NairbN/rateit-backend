package com.rateit.rateit_backend.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface VideoStorage {
    String store(Long postId, MultipartFile file);

    Resource load(String videoKey);
}
