package com.fiapql.notification.dto

import java.util.UUID

/** Mesmo payload publicado pela video-api. */
data class VideoJobMessage(
    val videoId: UUID,
    val userId: String,
    val minioKey: String,
    val userEmail: String
)
