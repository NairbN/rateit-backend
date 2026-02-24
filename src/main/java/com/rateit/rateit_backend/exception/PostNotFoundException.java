package com.rateit.rateit_backend.exception;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(Long postId) {
        super("Post not found with ID: " + postId);
    }
}
