package com.fiapql.videoapi.service

import com.fiapql.videoapi.dto.VideoJobMessage
import com.fiapql.videoapi.dto.VideoResponse
import com.fiapql.videoapi.entity.Video
import com.fiapql.videoapi.entity.VideoStatus
import com.fiapql.videoapi.repository.VideoRepository
import io.minio.BucketExistsArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.http.Method
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class VideoService(
    private val videoRepository: VideoRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val minioClient: MinioClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${minio.bucket.videos:videos}")
    private lateinit var videoBucket: String

    @Value("\${minio.bucket.processed:processed}")
    private lateinit var processedBucket: String

    @Value("\${rabbitmq.exchange:video.ex}")
    private lateinit var exchange: String

    @Value("\${rabbitmq.routing-key:video.process}")
    private lateinit var routingKey: String

    /** Upload: salva no MinIO → persiste no Postgres → publica job → retorna 202 */
    @CacheEvict(value = ["videos"], key = "#userId")
    fun upload(file: MultipartFile, userId: String, userEmail: String): VideoResponse {
        ensureBuckets()

        val minioKey = "videos/${UUID.randomUUID()}/${file.originalFilename}"

        // 1. Salva o vídeo bruto no MinIO
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(videoBucket)
                .`object`(minioKey)
                .stream(file.inputStream, file.size, -1)
                .contentType(file.contentType)
                .build()
        )

        // 2. Persiste metadados com status PENDING
        var video = Video(
            userId = userId,
            originalFilename = file.originalFilename ?: "video",
            minioKey = minioKey,
            status = VideoStatus.PENDING
        )
        video = videoRepository.save(video)

        // 3. Publica job na fila (publisher-confirms garante entrega)
        val msg = VideoJobMessage(video.id!!, userId, minioKey, userEmail)
        rabbitTemplate.convertAndSend(exchange, routingKey, msg)
        log.info("Job publicado: videoId={}", video.id)

        return toResponse(video)
    }

    /** Listagem com cache Redis por userId */
    @Cacheable(value = ["videos"], key = "#userId")
    fun listByUser(userId: String): List<VideoResponse> =
        videoRepository.findByUserIdOrderByCreatedAtDesc(userId).map { toResponse(it) }

    /** Status individual */
    fun getStatus(videoId: UUID, userId: String): VideoResponse {
        val video = videoRepository.findById(videoId)
            .filter { it.userId == userId }
            .orElseThrow { IllegalArgumentException("Vídeo não encontrado") }
        return toResponse(video)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun toResponse(v: Video): VideoResponse {
        val downloadUrl = v.zipKey
            ?.takeIf { v.status == VideoStatus.COMPLETED }
            ?.let { generatePresignedUrl(it) }
        return VideoResponse(v.id, v.originalFilename, v.status, downloadUrl, v.createdAt, v.updatedAt)
    }

    private fun generatePresignedUrl(zipKey: String): String? =
        try {
            minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .bucket(processedBucket)
                    .`object`(zipKey)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build()
            )
        } catch (e: Exception) {
            log.error("Falha ao gerar URL pré-assinada: {}", e.message)
            null
        }

    private fun ensureBuckets() {
        for (bucket in listOf(videoBucket, processedBucket)) {
            val exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
            if (!exists) minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
    }
}
