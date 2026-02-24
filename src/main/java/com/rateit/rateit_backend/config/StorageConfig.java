package com.rateit.rateit_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rateit.rateit_backend.storage.VideoStorage;

import org.springframework.beans.factory.annotation.Value;
import com.rateit.rateit_backend.storage.LocalVideoStorage;

@Configuration
public class StorageConfig {

    @Value("${app.storage.location}")
    private String storageLocation;

    @Bean
    public VideoStorage videoStorage() {
        return new LocalVideoStorage(storageLocation);
    }
}
