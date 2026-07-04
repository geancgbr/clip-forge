package com.fiapql.videoapi.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    /** Converte mensagens para JSON automaticamente */
    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** RabbitTemplate com publisher-confirms e conversor JSON */
    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        var template = new RabbitTemplate(cf);
        template.setMessageConverter(messageConverter());
        template.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                // Em produção: log + reenvio / alerta
                System.err.println("Mensagem não confirmada pelo broker: " + cause);
            }
        });
        return template;
    }
}
