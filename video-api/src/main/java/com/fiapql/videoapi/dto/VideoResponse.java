package com.fiapql.videoapi.dto;

import com.fiapql.videoapi.entity.VideoStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoResponse(
    UUID          id,
    String        originalFilename,
    VideoStatus   status,
    String        downloadUrl,       // preenchido apenas quando COMPLETED
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
