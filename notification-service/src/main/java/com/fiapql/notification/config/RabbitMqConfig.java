package com.fiapql.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    /** DLQ declarada aqui também para garantir que exista ao subir o serviço */
    @Bean
    Queue dlqQueue() {
        return QueueBuilder.durable("video.dlq").build();
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** Container factory com ACK manual (mesmo padrão do worker) */
    @Bean
    SimpleRabbitListenerContainerFactory manualAckFactory(ConnectionFactory cf) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
