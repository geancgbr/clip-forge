package com.fiapql.notification.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig {

    @Bean
    fun dlqQueue(): Queue = QueueBuilder.durable("video.dlq").build()

    // ObjectMapper com módulo Kotlin: necessário para desserializar a data class da mensagem
    @Bean
    fun messageConverter() = Jackson2JsonMessageConverter(jacksonObjectMapper())

    @Bean
    fun manualAckFactory(cf: ConnectionFactory): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        factory.setConnectionFactory(cf)
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL)
        factory.setPrefetchCount(1)
        factory.setMessageConverter(messageConverter())
        return factory
    }
}
