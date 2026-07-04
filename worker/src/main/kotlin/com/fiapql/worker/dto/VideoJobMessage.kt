package com.fiapql.worker.dto

import java.util.UUID

/**
 * Payload publicado pela video-api na fila video.process.
 * Mantém apenas o mínimo necessário — o vídeo em si fica no MinIO.
 */
data class VideoJobMessage(
    val videoId: UUID,
    val userId: String,
    val minioKey: String,    // caminho do vídeo bruto em MinIO: "videos/{videoId}.mp4"
    val userEmail: String    // para notificação de erro
)
