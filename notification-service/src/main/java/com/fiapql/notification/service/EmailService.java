package com.fiapql.notification.service;

import com.fiapql.notification.dto.VideoJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}") private String from;

    public void sendFailureNotification(VideoJobMessage msg) {
        try {
            var email = new SimpleMailMessage();
            email.setFrom(from);
            email.setTo(msg.userEmail());
            email.setSubject("Falha ao processar seu vídeo — FIAP X");
            email.setText(buildBody(msg));
            mailSender.send(email);
            log.info("E-mail de falha enviado para {} (videoId={})", msg.userEmail(), msg.videoId());
        } catch (Exception ex) {
            // Não relança: se o e-mail falhar não devemos travar o consumidor
            log.error("Falha ao enviar e-mail para {}: {}", msg.userEmail(), ex.getMessage());
        }
    }

    private String buildBody(VideoJobMessage msg) {
        return """
            Olá,

            Infelizmente não foi possível processar o vídeo "%s" após múltiplas tentativas.

            Detalhes:
              • ID do vídeo : %s
              • Arquivo     : %s

            Por favor, tente fazer o upload novamente. Se o problema persistir, entre em contato com o suporte.

            Atenciosamente,
            Equipe FIAP X
            """.formatted(
                msg.minioKey().replaceAll(".*/(.*)", "$1"),
                msg.videoId(),
                msg.minioKey()
            );
    }
}
