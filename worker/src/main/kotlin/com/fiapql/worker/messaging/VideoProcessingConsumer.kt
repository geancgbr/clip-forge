package com.fiapql.worker.messaging

import com.fiapql.worker.dto.VideoJobMessage
import com.fiapql.worker.service.VideoProcessingService
import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class VideoProcessingConsumer(private val processingService: VideoProcessingService) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * prefetch = 1  →  um vídeo por vez por worker.
     * acknowledgeMode = MANUAL  →  ACK/NACK controlados manualmente abaixo.
     * containerFactory definida em RabbitMqConfig.
     */
    @RabbitListener(
        queues = ["\${rabbitmq.queues.video-process:video.process}"],
        containerFactory = "manualAckFactory"
    )
    fun onVideoJob(
        message: VideoJobMessage,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long
    ) {
        log.info("Job recebido: videoId={}", message.videoId)

        try {
            processingService.process(message)

            // Sucesso → ACK: mensagem removida da fila
            channel.basicAck(deliveryTag, false)
            log.info("ACK: videoId={}", message.videoId)
        } catch (ex: Exception) {
            log.error("Falha ao processar videoId={}: {}", message.videoId, ex.message, ex)

            /*
             * NACK com requeue=false:
             *   - RabbitMQ NÃO devolve a mensagem para a mesma fila
             *   - x-dead-letter-exchange roteia para video.retry.ex
             *   - RetryRouterConsumer lê x-death e decide: retry 30s/2m/10m ou DLQ
             */
            try {
                channel.basicNack(deliveryTag, false, false)
            } catch (nackEx: Exception) {
                log.error("Falha ao enviar NACK: {}", nackEx.message, nackEx)
            }
        }
    }
}
