package com.fiapql.notification.messaging;

import com.fiapql.notification.dto.VideoJobMessage;
import com.fiapql.notification.service.EmailService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private final EmailService emailService;

    /**
     * Consome video.dlq — mensagens que falharam 3x no worker.
     * ACK sempre ao final: o e-mail pode falhar silenciosamente (log),
     * mas a mensagem não deve voltar para a fila.
     */
    @RabbitListener(queues = "${rabbitmq.queues.dlq:video.dlq}",
                    containerFactory = "manualAckFactory")
    public void onFailedJob(
            VideoJobMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.warn("Mensagem na DLQ: videoId={} userEmail={}", message.videoId(), message.userEmail());

        try {
            emailService.sendFailureNotification(message);
        } finally {
            // ACK incondicional: e-mail já foi tentado, não há mais o que fazer com a mensagem
            try {
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                log.error("Falha ao enviar ACK na DLQ: {}", e.getMessage());
            }
        }
    }
}
