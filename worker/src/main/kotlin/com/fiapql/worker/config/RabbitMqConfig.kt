package com.fiapql.worker.config

import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig {

    @Bean
    fun videoExchange() = DirectExchange("video.ex", true, false)

    @Bean
    fun retryExchange() = DirectExchange("video.retry.ex", true, false)

    @Bean
    fun dlxExchange() = DirectExchange("video.dlx", true, false)


    @Bean
    fun videoProcessQueue(): Queue =
        QueueBuilder.durable("video.process")
            .quorum()
            .withArgument("x-dead-letter-exchange", "video.retry.ex")
            .withArgument("x-dead-letter-routing-key", "video.retry")
            .build()

    /** Retry 30 s: ao expirar, devolve ao exchange principal */
    @Bean
    fun retryQueue30s(): Queue =
        QueueBuilder.durable("video.retry.30s")
            .withArguments(
                mapOf<String, Any>(
                    "x-message-ttl" to 30_000,
                    "x-dead-letter-exchange" to "video.ex",
                    "x-dead-letter-routing-key" to "video.process"
                )
            ).build()

    @Bean
    fun retryQueue2m(): Queue =
        QueueBuilder.durable("video.retry.2m")
            .withArguments(
                mapOf<String, Any>(
                    "x-message-ttl" to 120_000,
                    "x-dead-letter-exchange" to "video.ex",
                    "x-dead-letter-routing-key" to "video.process"
                )
            ).build()

    @Bean
    fun retryQueue10m(): Queue =
        QueueBuilder.durable("video.retry.10m")
            .withArguments(
                mapOf<String, Any>(
                    "x-message-ttl" to 600_000,
                    "x-dead-letter-exchange" to "video.ex",
                    "x-dead-letter-routing-key" to "video.process"
                )
            ).build()

    @Bean
    fun dlqQueue(): Queue = QueueBuilder.durable("video.dlq").build()

    @Bean
    fun videoProcessBinding(): Binding =
        BindingBuilder.bind(videoProcessQueue()).to(videoExchange()).with("video.process")

    @Bean
    fun retry30sBinding(): Binding =
        BindingBuilder.bind(retryQueue30s()).to(retryExchange()).with("video.retry.30s")

    @Bean
    fun retry2mBinding(): Binding =
        BindingBuilder.bind(retryQueue2m()).to(retryExchange()).with("video.retry.2m")

    @Bean
    fun retry10mBinding(): Binding =
        BindingBuilder.bind(retryQueue10m()).to(retryExchange()).with("video.retry.10m")

    @Bean
    fun dlqBinding(): Binding =
        BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with("video.dlq")

    // ── Container factory com ack MANUAL e prefetch 1 ─────────────────────────

    @Bean
    fun manualAckFactory(cf: ConnectionFactory): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        factory.setConnectionFactory(cf)
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL)
        factory.setPrefetchCount(1)
        factory.setMessageConverter(jackson2JsonMessageConverter())
        return factory
    }

    @Bean
    fun jackson2JsonMessageConverter() = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitAdmin(cf: ConnectionFactory) = RabbitAdmin(cf)
}
