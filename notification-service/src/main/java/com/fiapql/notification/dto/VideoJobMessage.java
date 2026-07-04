package com.fiapql.notification.dto;

import java.util.UUID;

/** Mesmo payload publicado pela video-api. */
public record VideoJobMessage(
    UUID   videoId,
    String userId,
    String minioKey,
    String userEmail
) {}
