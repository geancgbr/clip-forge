package com.fiapql.worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMqConfig {

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Bean DirectExchange videoExchange() {
        return new DirectExchange("video.ex", true, false);
    }

    @Bean DirectExchange retryExchange() {
        return new DirectExchange("video.retry.ex", true, false);
    }

    @Bean DirectExchange dlxExchange() {
        return new DirectExchange("video.dlx", true, false);
    }

    // ── Filas ─────────────────────────────────────────────────────────────────

    /** Fila principal: quorum, DLX apontando para retry */
    @Bean Queue videoProcessQueue() {
        return QueueBuilder.durable("video.process")
            .quorum()
            .withArgument("x-dead-letter-exchange", "video.retry.ex")
            .withArgument("x-dead-letter-routing-key", "video.retry")
            .build();
    }

    /** Retry 30 s: ao expirar, devolve ao exchange principal */
    @Bean Queue retryQueue30s() {
        return QueueBuilder.durable("video.retry.30s")
            .withArguments(Map.of(
                "x-message-ttl",            30_000,
                "x-dead-letter-exchange",   "video.ex",
                "x-dead-letter-routing-key","video.process"
            )).build();
    }

    /** Retry 2 min */
    @Bean Queue retryQueue2m() {
        return QueueBuilder.durable("video.retry.2m")
            .withArguments(Map.of(
                "x-message-ttl",            120_000,
                "x-dead-letter-exchange",   "video.ex",
                "x-dead-letter-routing-key","video.process"
            )).build();
    }

    /** Retry 10 min */
    @Bean Queue retryQueue10m() {
        return QueueBuilder.durable("video.retry.10m")
            .withArguments(Map.of(
                "x-message-ttl",            600_000,
                "x-dead-letter-exchange",   "video.ex",
                "x-dead-letter-routing-key","video.process"
            )).build();
    }

    /** DLQ: mensagens sem mais tentativas */
    @Bean Queue dlqQueue() {
        return QueueBuilder.durable("video.dlq").build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @Bean Binding videoProcessBinding()  { return BindingBuilder.bind(videoProcessQueue()).to(videoExchange()).with("video.process"); }
    @Bean Binding retry30sBinding()      { return BindingBuilder.bind(retryQueue30s()).to(retryExchange()).with("video.retry.30s"); }
    @Bean Binding retry2mBinding()       { return BindingBuilder.bind(retryQueue2m()).to(retryExchange()).with("video.retry.2m"); }
    @Bean Binding retry10mBinding()      { return BindingBuilder.bind(retryQueue10m()).to(retryExchange()).with("video.retry.10m"); }
    @Bean Binding dlqBinding()           { return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with("video.dlq"); }

    // ── Container factory com ack MANUAL e prefetch 1 ─────────────────────────

    @Bean
    SimpleRabbitListenerContainerFactory manualAckFactory(ConnectionFactory cf) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        return factory;
    }

    @Bean Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean RabbitAdmin rabbitAdmin(ConnectionFactory cf) {
        return new RabbitAdmin(cf);
    }
}
