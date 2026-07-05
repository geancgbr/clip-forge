package com.fiapql.videoapi.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMqConfig {

    // O produtor também declara a topologia (idempotente, mesmos argumentos do
    // worker): sem isso, publicar antes de o worker subir descarta a mensagem
    @Bean
    fun videoExchange() = DirectExchange("video.ex", true, false)

    @Bean
    fun videoProcessQueue(): Queue =
        QueueBuilder.durable("video.process")
            .quorum()
            .withArgument("x-dead-letter-exchange", "video.retry.ex")
            .withArgument("x-dead-letter-routing-key", "video.retry")
            .build()

    @Bean
    fun videoProcessBinding(): Binding =
        BindingBuilder.bind(videoProcessQueue()).to(videoExchange()).with("video.process")

    @Bean
    fun rabbitAdmin(cf: ConnectionFactory) = RabbitAdmin(cf)

    @Bean
    fun messageConverter() = Jackson2JsonMessageConverter()

    /** RabbitTemplate com publisher-confirms e conversor JSON */
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
