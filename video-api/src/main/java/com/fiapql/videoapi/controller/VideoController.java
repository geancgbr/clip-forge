package com.fiapql.videoapi.controller;

import com.fiapql.videoapi.dto.VideoResponse;
import com.fiapql.videoapi.entity.User;
import com.fiapql.videoapi.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * POST /videos/upload
     * Recebe o vídeo como multipart, salva no MinIO, publica o job e retorna 202.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws Exception {

        var resp = videoService.upload(file, user.getId().toString(), user.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
    }

    /**
     * GET /videos
     * Lista todos os vídeos do usuário autenticado com seus status.
     */
    @GetMapping
    public ResponseEntity<List<VideoResponse>> list(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(videoService.listByUser(user.getId().toString()));
    }

    /**
     * GET /videos/{id}
     * Status de um vídeo específico; retorna downloadUrl quando COMPLETED.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getStatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(videoService.getStatus(id, user.getId().toString()));
    }
}
