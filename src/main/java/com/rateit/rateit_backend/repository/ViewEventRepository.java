package com.rateit.rateit_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rateit.rateit_backend.entity.ViewEvent;

@Repository
public interface ViewEventRepository extends JpaRepository<ViewEvent, Long> {
    long countByPostId(Long postId);
}