package com.fiapql.videoapi.config

import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig {

    @Bean
    fun messageConverter() = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(cf: ConnectionFactory): RabbitTemplate {
        val template = RabbitTemplate(cf)
        template.messageConverter = messageConverter()
        template.setConfirmCallback { _, ack, cause ->
            if (!ack) {
                // Em produção: log + reenvio / alerta
                System.err.println("Mensagem não confirmada pelo broker: $cause")
            }
        }
        return template
    }
}
