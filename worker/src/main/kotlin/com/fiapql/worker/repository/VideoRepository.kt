package com.fiapql.worker.repository

import com.fiapql.worker.entity.Video
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VideoRepository : JpaRepository<Video, UUID>
