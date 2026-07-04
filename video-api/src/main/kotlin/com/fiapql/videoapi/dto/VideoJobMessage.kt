package com.fiapql.videoapi.dto

import java.util.UUID

data class VideoJobMessage(
    val videoId: UUID,
    val userId: String,
    val minioKey: String,
    val userEmail: String
)
