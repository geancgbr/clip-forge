package com.fiapql.videoapi.service;

import com.fiapql.videoapi.dto.*;
import com.fiapql.videoapi.entity.*;
import com.fiapql.videoapi.repository.VideoRepository;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final RabbitTemplate  rabbitTemplate;
    private final MinioClient     minioClient;

    @Value("${minio.bucket.videos:videos}")      private String videoBucket;
    @Value("${minio.bucket.processed:processed}") private String processedBucket;
    @Value("${rabbitmq.exchange:video.ex}")       private String exchange;
    @Value("${rabbitmq.routing-key:video.process}") private String routingKey;

    /** Upload: salva no MinIO → persiste no Postgres → publica job → retorna 202 */
    @CacheEvict(value = "videos", key = "#userId")
    public VideoResponse upload(MultipartFile file, String userId, String userEmail) throws Exception {
        ensureBuckets();

        String minioKey = "videos/" + UUID.randomUUID() + "/" + file.getOriginalFilename();

        // 1. Salva o vídeo bruto no MinIO
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(videoBucket)
            .object(minioKey)
            .stream(file.getInputStream(), file.getSize(), -1)
            .contentType(file.getContentType())
            .build());

        // 2. Persiste metadados com status PENDING
        var video = Video.builder()
            .userId(userId)
            .originalFilename(file.getOriginalFilename())
            .minioKey(minioKey)
            .status(VideoStatus.PENDING)
            .build();
        video = videoRepository.save(video);

        // 3. Publica job na fila (publisher-confirms garante entrega)
        var msg = new VideoJobMessage(video.getId(), userId, minioKey, userEmail);
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
        log.info("Job publicado: videoId={}", video.getId());

        return toResponse(video);
    }

    /** Listagem com cache Redis por userId */
    @Cacheable(value = "videos", key = "#userId")
    public List<VideoResponse> listByUser(String userId) {
        return videoRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::toResponse).toList();
    }

    /** Status individual */
    public VideoResponse getStatus(UUID videoId, String userId) {
        var video = videoRepository.findById(videoId)
            .filter(v -> v.getUserId().equals(userId))
            .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado"));
        return toResponse(video);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private VideoResponse toResponse(Video v) {
        String downloadUrl = null;
        if (v.getStatus() == VideoStatus.COMPLETED && v.getZipKey() != null) {
            downloadUrl = generatePresignedUrl(v.getZipKey());
        }
        return new VideoResponse(v.getId(), v.getOriginalFilename(),
            v.getStatus(), downloadUrl, v.getCreatedAt(), v.getUpdatedAt());
    }

    private String generatePresignedUrl(String zipKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(processedBucket)
                .object(zipKey)
                .method(Method.GET)
                .expiry(1, TimeUnit.HOURS)
                .build());
        } catch (Exception e) {
            log.error("Falha ao gerar URL pré-assinada: {}", e.getMessage());
            return null;
        }
    }

    private void ensureBuckets() throws Exception {
        for (String bucket : List.of(videoBucket, processedBucket)) {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
