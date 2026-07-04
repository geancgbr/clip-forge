package com.fiapql.worker.dto

import java.util.UUID

data class VideoJobMessage(
    val videoId: UUID,
    val userId: String,
    val minioKey: String,
    val userEmail: String
)
