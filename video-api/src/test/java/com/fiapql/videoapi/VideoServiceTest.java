package com.fiapql.videoapi;

import com.fiapql.videoapi.dto.VideoJobMessage;
import com.fiapql.videoapi.entity.*;
import com.fiapql.videoapi.repository.VideoRepository;
import com.fiapql.videoapi.service.VideoService;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock VideoRepository videoRepository;
    @Mock RabbitTemplate  rabbitTemplate;
    @Mock MinioClient     minioClient;

    @InjectMocks VideoService videoService;

    @Test
    void listByUser_deveRetornarVideosDoUsuario() {
        var userId = UUID.randomUUID().toString();
        var video  = Video.builder()
            .id(UUID.randomUUID()).userId(userId)
            .originalFilename("test.mp4").minioKey("videos/test.mp4")
            .status(VideoStatus.PENDING).build();

        when(videoRepository.findByUserIdOrderByCreatedAtDesc(userId))
            .thenReturn(List.of(video));

        var result = videoService.listByUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(VideoStatus.PENDING);
    }

    @Test
    void getStatus_videoDeOutroUsuario_deveLancarExcecao() {
        var videoId  = UUID.randomUUID();
        var video    = Video.builder().id(videoId).userId("owner-id").status(VideoStatus.PENDING).build();

        when(videoRepository.findById(videoId)).thenReturn(java.util.Optional.of(video));

        assertThatThrownBy(() -> videoService.getStatus(videoId, "outro-usuario"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
