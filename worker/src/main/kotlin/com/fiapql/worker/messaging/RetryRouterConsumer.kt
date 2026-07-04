package com.fiapql.worker.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fiapql.worker.dto.VideoJobMessage
import com.fiapql.worker.entity.VideoStatus
import com.fiapql.worker.repository.VideoRepository
import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class RetryRouterConsumer(
    private val rabbitTemplate: RabbitTemplate,
    private val videoRepository: VideoRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Recebe as mensagens rejeitadas pela fila principal (NACK → video.retry.ex / video.retry),
     * lê o header x-death e decide o destino:
     *   1ª falha → video.retry.30s · 2ª → video.retry.2m · 3ª → video.retry.10m · 4ª+ → video.dlq
     * As filas de retry têm TTL; ao expirar, a mensagem volta para video.process.
     */
    @RabbitListener(queues = ["video.retry"], containerFactory = "manualAckFactory")
    fun route(
        message: Message,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long
    ) {
        try {
            val failures = rejectionCount(message)
            val retryKey = when (failures) {
                1L -> "video.retry.30s"
                2L -> "video.retry.2m"
                3L -> "video.retry.10m"
                else -> null
            }

            if (retryKey != null) {
                rabbitTemplate.send("video.retry.ex", retryKey, message)
                log.info("Falha nº {} → agendado retry em {}", failures, retryKey)
            } else {
                markAsFailed(message)
                rabbitTemplate.send("video.dlx", "video.dlq", message)
                log.warn("Falha nº {} → video.dlq, sem mais tentativas", failures)
            }
            channel.basicAck(deliveryTag, false)
        } catch (ex: Exception) {
            log.error("Falha ao rotear retry: {}", ex.message, ex)
            // requeue=true: o roteador não pode perder a mensagem
            channel.basicNack(deliveryTag, false, true)
        }
    }

    /** Quantas vezes a mensagem já morreu na fila principal (header x-death do RabbitMQ) */
    private fun rejectionCount(message: Message): Long =
        message.messageProperties.xDeathHeader
            ?.firstOrNull { it["queue"] == "video.process" }
            ?.let { (it["count"] as? Number)?.toLong() }
            ?: 1L

    /** Última tentativa esgotada: reflete a falha no banco para o usuário ver o status */
    private fun markAsFailed(message: Message) {
        try {
            val job = objectMapper.readValue(message.body, VideoJobMessage::class.java)
            videoRepository.findById(job.videoId).ifPresent { video ->
                video.status = VideoStatus.FAILED
                videoRepository.save(video)
            }
        } catch (ex: Exception) {
            log.error("Não foi possível marcar o vídeo como FAILED: {}", ex.message)
        }
    }
}
