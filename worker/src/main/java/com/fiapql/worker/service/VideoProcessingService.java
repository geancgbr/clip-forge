package com.fiapql.worker.service;

import com.fiapql.worker.dto.VideoJobMessage;
import com.fiapql.worker.entity.Video;
import com.fiapql.worker.entity.VideoStatus;
import com.fiapql.worker.repository.VideoRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessingService {

    private final MinioClient     minioClient;
    private final VideoRepository videoRepository;

    @Value("${minio.bucket.videos:videos}")     private String videoBucket;
    @Value("${minio.bucket.processed:processed}") private String processedBucket;

    public void process(VideoJobMessage msg) throws Exception {
        var video = videoRepository.findById(msg.videoId())
            .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado: " + msg.videoId()));

        // 1. Marca como PROCESSING (idempotente: só avança se estiver PENDING)
        if (video.getStatus() != VideoStatus.PENDING) {
            log.warn("Vídeo {} já está {}, ignorando", msg.videoId(), video.getStatus());
            return;
        }
        video.setStatus(VideoStatus.PROCESSING);
        videoRepository.save(video);

        Path tempDir = Files.createTempDirectory("fiapx-" + msg.videoId());
        try {
            // 2. Baixa o vídeo do MinIO
            Path videoFile = tempDir.resolve("input.mp4");
            try (var stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(videoBucket).object(msg.minioKey()).build())) {
                Files.copy(stream, videoFile);
            }

            // 3. Extrai frames com FFmpeg (1 frame por segundo)
            Path framesDir = tempDir.resolve("frames");
            Files.createDirectory(framesDir);
            runFfmpeg(videoFile, framesDir);

            // 4. Gera ZIP dos frames e faz upload para MinIO
            String zipKey = "processed/" + msg.videoId() + "/frames.zip";
            Path zipFile  = tempDir.resolve("frames.zip");
            zipDirectory(framesDir, zipFile);

            try (var is = Files.newInputStream(zipFile)) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(processedBucket)
                        .object(zipKey)
                        .stream(is, Files.size(zipFile), -1)
                        .contentType("application/zip")
                        .build());
            }

            // 5. Marca COMPLETED
            video.setStatus(VideoStatus.COMPLETED);
            video.setZipKey(zipKey);
            videoRepository.save(video);
            log.info("Vídeo {} processado com sucesso", msg.videoId());

        } finally {
            deleteTempDir(tempDir);
        }
    }

    private void runFfmpeg(Path videoFile, Path framesDir) throws IOException, InterruptedException {
        var cmd = new ProcessBuilder(
            "ffmpeg", "-i", videoFile.toString(),
            "-vf", "fps=1",
            framesDir.resolve("frame_%04d.png").toString()
        ).redirectErrorStream(true).start();

        // consome stdout para não travar
        try (var reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()))) {
            reader.lines().forEach(line -> log.debug("[ffmpeg] {}", line));
        }

        int exit = cmd.waitFor();
        if (exit != 0) throw new RuntimeException("FFmpeg falhou com código " + exit);
    }

    private void zipDirectory(Path sourceDir, Path zipPath) throws IOException {
        try (var zos = new ZipOutputStream(Files.newOutputStream(zipPath));
             var files = Files.walk(sourceDir)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    zos.putNextEntry(new ZipEntry(sourceDir.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
        }
    }

    private void deleteTempDir(Path dir) {
        try (var files = Files.walk(dir)) {
            files.sorted(java.util.Comparator.reverseOrder())
                 .map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            log.warn("Falha ao limpar tmpdir {}: {}", dir, e.getMessage());
        }
    }
}
