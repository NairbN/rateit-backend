package com.rateit.rateit_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rateit.rateit_backend.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}
