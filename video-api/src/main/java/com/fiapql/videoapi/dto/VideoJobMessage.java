package com.fiapql.videoapi.dto;

import java.util.UUID;

public record VideoJobMessage(
    UUID   videoId,
    String userId,
    String minioKey,
    String userEmail
) {}
