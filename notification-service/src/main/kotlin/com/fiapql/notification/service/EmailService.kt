package com.fiapql.notification.service

import com.fiapql.notification.dto.VideoJobMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class EmailService(private val mailSender: JavaMailSender) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${spring.mail.username}")
    private lateinit var from: String

    fun sendFailureNotification(msg: VideoJobMessage) {
        try {
            val email = SimpleMailMessage()
            email.from = from
            email.setTo(msg.userEmail)
            email.subject = "Falha ao processar seu vídeo — FIAP X"
            email.text = buildBody(msg)
            mailSender.send(email)
            log.info("E-mail de falha enviado para {} (videoId={})", msg.userEmail, msg.videoId)
        } catch (ex: Exception) {
            // Não relança: se o e-mail falhar não devemos travar o consumidor
            log.error("Falha ao enviar e-mail para {}: {}", msg.userEmail, ex.message)
        }
    }

    private fun buildBody(msg: VideoJobMessage) = """
        Olá,

        Infelizmente não foi possível processar o vídeo "${msg.minioKey.substringAfterLast('/')}" após múltiplas tentativas.

        Detalhes:
          • ID do vídeo : ${msg.videoId}
          • Arquivo     : ${msg.minioKey}

        Por favor, tente fazer o upload novamente. Se o problema persistir, entre em contato com o suporte.

        Atenciosamente,
        Equipe FIAP X
    """.trimIndent()
}
