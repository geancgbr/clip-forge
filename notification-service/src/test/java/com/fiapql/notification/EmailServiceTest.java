package com.fiapql.notification;

import com.fiapql.notification.dto.VideoJobMessage;
import com.fiapql.notification.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks EmailService emailService;

    @Test
    void sendFailureNotification_deveEnviarEmail() {
        ReflectionTestUtils.setField(emailService, "from", "noreply@fiapx.com");

        var msg = new VideoJobMessage(
            UUID.randomUUID(), "user-1",
            "videos/abc/video.mp4", "gean@fiapx.com"
        );

        emailService.sendFailureNotification(msg);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendFailureNotification_falhaNoEnvio_naoDeveLancarExcecao() {
        ReflectionTestUtils.setField(emailService, "from", "noreply@fiapx.com");
        doThrow(new RuntimeException("SMTP offline")).when(mailSender).send(any(SimpleMailMessage.class));

        var msg = new VideoJobMessage(
            UUID.randomUUID(), "user-1",
            "videos/abc/video.mp4", "gean@fiapx.com"
        );

        // não deve propagar exceção — consumer deve dar ACK mesmo assim
        emailService.sendFailureNotification(msg);
    }
}
