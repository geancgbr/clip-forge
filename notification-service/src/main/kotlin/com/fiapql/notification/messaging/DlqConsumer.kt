package com.fiapql.notification.messaging

import com.fiapql.notification.dto.VideoJobMessage
import com.fiapql.notification.service.EmailService
import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.support.AmqpHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class DlqConsumer(private val emailService: EmailService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(
        queues = ["\${rabbitmq.queues.dlq:video.dlq}"],
        containerFactory = "manualAckFactory"
    )
    fun onFailedJob(
        message: VideoJobMessage,
        channel: Channel,
        @Header(AmqpHeaders.DELIVERY_TAG) deliveryTag: Long
    ) {
        log.warn("Mensagem na DLQ: videoId={} userEmail={}", message.videoId, message.userEmail)

        try {
            emailService.sendFailureNotification(message)
        } finally {
            // ACK incondicional: e-mail já foi tentado, não há mais o que fazer com a mensagem
            try {
                channel.basicAck(deliveryTag, false)
            } catch (e: Exception) {
                log.error("Falha ao enviar ACK na DLQ: {}", e.message)
            }
        }
    }
}
