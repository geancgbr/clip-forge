package com.fiapql.videoapi.repository;

import com.fiapql.videoapi.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {
    List<Video> findByUserIdOrderByCreatedAtDesc(String userId);
}
