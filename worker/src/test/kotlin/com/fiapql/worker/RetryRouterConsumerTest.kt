package com.fiapql.worker

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fiapql.worker.dto.VideoJobMessage
import com.fiapql.worker.entity.Video
import com.fiapql.worker.entity.VideoStatus
import com.fiapql.worker.messaging.RetryRouterConsumer
import com.fiapql.worker.repository.VideoRepository
import com.rabbitmq.client.Channel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RetryRouterConsumerTest {

    @Mock lateinit var rabbitTemplate: RabbitTemplate
    @Mock lateinit var videoRepository: VideoRepository
    @Mock lateinit var channel: Channel

    private val objectMapper = jacksonObjectMapper()
    private lateinit var router: RetryRouterConsumer

    private val videoId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        router = RetryRouterConsumer(rabbitTemplate, videoRepository, objectMapper)
    }

    private fun message(rejections: Long?): Message {
        val props = MessageProperties()
        if (rejections != null) {
            props.setHeader(
                "x-death",
                listOf(mapOf("queue" to "video.process", "reason" to "rejected", "count" to rejections))
            )
        }
        val body = objectMapper.writeValueAsBytes(
            VideoJobMessage(videoId, "user-1", "videos/x/video.mp4", "gean@fiapx.com")
        )
        return Message(body, props)
    }

    @Test
    fun `primeira falha deve agendar retry de 30s`() {
        val msg = message(rejections = 1)

        router.route(msg, channel, 7L)

        verify(rabbitTemplate).send(eq("video.retry.ex"), eq("video.retry.30s"), eq(msg))
        verify(channel).basicAck(7L, false)
        verify(videoRepository, never()).save(any())
    }

    @Test
    fun `terceira falha deve agendar retry de 10m`() {
        val msg = message(rejections = 3)

        router.route(msg, channel, 7L)

        verify(rabbitTemplate).send(eq("video.retry.ex"), eq("video.retry.10m"), eq(msg))
        verify(channel).basicAck(7L, false)
    }

    @Test
    fun `quarta falha deve ir para a DLQ e marcar o video como FAILED`() {
        val video = Video(id = videoId, userId = "user-1", status = VideoStatus.PROCESSING)
        whenever(videoRepository.findById(videoId)).thenReturn(Optional.of(video))
        val msg = message(rejections = 4)

        router.route(msg, channel, 7L)

        verify(rabbitTemplate).send(eq("video.dlx"), eq("video.dlq"), eq(msg))
        val captor = argumentCaptor<Video>()
        verify(videoRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo(VideoStatus.FAILED)
        verify(channel).basicAck(7L, false)
    }

    @Test
    fun `mensagem sem x-death conta como primeira falha`() {
        val msg = message(rejections = null)

        router.route(msg, channel, 7L)

        verify(rabbitTemplate).send(eq("video.retry.ex"), eq("video.retry.30s"), eq(msg))
        verify(channel).basicAck(7L, false)
    }
}
