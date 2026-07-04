package com.fiapql.worker.messaging;

import com.fiapql.worker.dto.VideoJobMessage;
import com.fiapql.worker.service.VideoProcessingService;
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
public class VideoProcessingConsumer {

    private final VideoProcessingService processingService;

    /**
     * prefetch = 1  →  um vídeo por vez por worker.
     * acknowledgeMode = MANUAL  →  ACK/NACK controlados manualmente abaixo.
     * containerFactory definida em RabbitMqConfig.
     */
    @RabbitListener(
        queues           = "${rabbitmq.queues.video-process:video.process}",
        containerFactory = "manualAckFactory"
    )
    public void onVideoJob(
            VideoJobMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("Job recebido: videoId={}", message.videoId());

        try {
            processingService.process(message);

            // Sucesso → ACK: mensagem removida da fila
            channel.basicAck(deliveryTag, false);
            log.info("ACK: videoId={}", message.videoId());

        } catch (Exception ex) {
            log.error("Falha ao processar videoId={}: {}", message.videoId(), ex.getMessage(), ex);

            /*
             * NACK com requeue=false:
             *   - RabbitMQ NÃO devolve a mensagem para a mesma fila
             *   - x-dead-letter-exchange roteia para video.retry.ex
             *   - RetryRouterConsumer lê x-death e decide: retry 30s/2m/10m ou DLQ
             */
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackEx) {
                log.error("Falha ao enviar NACK: {}", nackEx.getMessage(), nackEx);
            }
        }
    }
}
