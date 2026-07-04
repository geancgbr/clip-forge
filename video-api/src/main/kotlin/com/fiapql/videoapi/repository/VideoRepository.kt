package com.fiapql.videoapi.repository

import com.fiapql.videoapi.entity.Video
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VideoRepository : JpaRepository<Video, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: String): List<Video>
}
