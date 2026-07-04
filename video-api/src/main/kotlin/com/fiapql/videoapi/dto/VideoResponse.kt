package com.fiapql.videoapi.dto

import com.fiapql.videoapi.entity.VideoStatus
import java.time.LocalDateTime
import java.util.UUID

data class VideoResponse(
    val id: UUID?,
    val originalFilename: String,
    val status: VideoStatus,
    val downloadUrl: String?,       // preenchido apenas quando COMPLETED
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)
