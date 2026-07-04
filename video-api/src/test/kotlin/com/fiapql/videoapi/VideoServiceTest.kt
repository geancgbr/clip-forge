package com.fiapql.videoapi

import com.fiapql.videoapi.entity.Video
import com.fiapql.videoapi.entity.VideoStatus
import com.fiapql.videoapi.repository.VideoRepository
import com.fiapql.videoapi.service.VideoService
import io.minio.MinioClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class VideoServiceTest {

    @Mock lateinit var videoRepository: VideoRepository
    @Mock lateinit var rabbitTemplate: RabbitTemplate
    @Mock lateinit var minioClient: MinioClient

    @InjectMocks lateinit var videoService: VideoService

    @Test
    fun `listByUser deve retornar videos do usuario`() {
        val userId = UUID.randomUUID().toString()
        val video = Video(
            id = UUID.randomUUID(),
            userId = userId,
            originalFilename = "test.mp4",
            minioKey = "videos/test.mp4",
            status = VideoStatus.PENDING
        )

        whenever(videoRepository.findByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(listOf(video))

        val result = videoService.listByUser(userId)

        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(VideoStatus.PENDING)
    }

    @Test
    fun `getStatus de video de outro usuario deve lancar excecao`() {
        val videoId = UUID.randomUUID()
        val video = Video(id = videoId, userId = "owner-id", status = VideoStatus.PENDING)

        whenever(videoRepository.findById(videoId)).thenReturn(Optional.of(video))

        assertThatThrownBy { videoService.getStatus(videoId, "outro-usuario") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
