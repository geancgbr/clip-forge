package com.fiapql.worker.repository;

import com.fiapql.worker.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {}
