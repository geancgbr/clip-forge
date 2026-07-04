package com.fiapql.worker.dto;

import java.util.UUID;

/**
 * Payload publicado pela video-api na fila video.process.
 * Mantém apenas o mínimo necessário — o vídeo em si fica no MinIO.
 */
public record VideoJobMessage(
    UUID   videoId,
    String userId,
    String minioKey,    // caminho do vídeo bruto em MinIO: "videos/{videoId}.mp4"
    String userEmail    // para notificação de erro
) {}
