package com.fiapql.notification

import com.fiapql.notification.dto.VideoJobMessage
import com.fiapql.notification.service.EmailService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class EmailServiceTest {

    @Mock lateinit var mailSender: JavaMailSender
    @InjectMocks lateinit var emailService: EmailService

    @Test
    fun `sendFailureNotification deve enviar email`() {
        ReflectionTestUtils.setField(emailService, "from", "noreply@fiapx.com")

        val msg = VideoJobMessage(
            UUID.randomUUID(), "user-1",
            "videos/abc/video.mp4", "gean@fiapx.com"
        )

        emailService.sendFailureNotification(msg)

        verify(mailSender).send(any<SimpleMailMessage>())
    }

    @Test
    fun `sendFailureNotification com falha no envio nao deve lancar excecao`() {
        ReflectionTestUtils.setField(emailService, "from", "noreply@fiapx.com")
        doThrow(RuntimeException("SMTP offline")).whenever(mailSender).send(any<SimpleMailMessage>())

        val msg = VideoJobMessage(
            UUID.randomUUID(), "user-1",
            "videos/abc/video.mp4", "gean@fiapx.com"
        )

        // não deve propagar exceção — consumer deve dar ACK mesmo assim
        emailService.sendFailureNotification(msg)
    }
}
