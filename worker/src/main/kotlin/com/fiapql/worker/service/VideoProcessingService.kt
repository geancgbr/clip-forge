package com.fiapql.worker.service

import com.fiapql.worker.dto.VideoJobMessage
import com.fiapql.worker.entity.VideoStatus
import com.fiapql.worker.repository.VideoRepository
import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class VideoProcessingService(
    private val minioClient: MinioClient,
    private val videoRepository: VideoRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${minio.bucket.videos:videos}")
    private lateinit var videoBucket: String

    @Value("\${minio.bucket.processed:processed}")
    private lateinit var processedBucket: String

    fun process(msg: VideoJobMessage) {
        val video = videoRepository.findById(msg.videoId)
            .orElseThrow { IllegalArgumentException("Vídeo não encontrado: ${msg.videoId}") }

        if (video.status != VideoStatus.PENDING) {
            log.warn("Vídeo {} já está {}, ignorando", msg.videoId, video.status)
            return
        }
        video.status = VideoStatus.PROCESSING
        videoRepository.save(video)

        val tempDir = Files.createTempDirectory("fiapx-${msg.videoId}")
        try {
            val videoFile = tempDir.resolve("input.mp4")
            minioClient.getObject(
                GetObjectArgs.builder().bucket(videoBucket).`object`(msg.minioKey).build()
            ).use { stream ->
                Files.copy(stream, videoFile)
            }

            val framesDir = tempDir.resolve("frames")
            Files.createDirectory(framesDir)
            runFfmpeg(videoFile, framesDir)

            val zipKey = "processed/${msg.videoId}/frames.zip"
            val zipFile = tempDir.resolve("frames.zip")
            zipDirectory(framesDir, zipFile)

            Files.newInputStream(zipFile).use { input ->
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(processedBucket)
                        .`object`(zipKey)
                        .stream(input, Files.size(zipFile), -1)
                        .contentType("application/zip")
                        .build()
                )
            }

            video.status = VideoStatus.COMPLETED
            video.zipKey = zipKey
            videoRepository.save(video)
            log.info("Vídeo {} processado com sucesso", msg.videoId)
        } finally {
            deleteTempDir(tempDir)
        }
    }

    private fun runFfmpeg(videoFile: Path, framesDir: Path) {
        val cmd = ProcessBuilder(
            "ffmpeg", "-i", videoFile.toString(),
            "-vf", "fps=1",
            framesDir.resolve("frame_%04d.png").toString()
        ).redirectErrorStream(true).start()

        cmd.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { log.debug("[ffmpeg] {}", it) }
        }

        val exit = cmd.waitFor()
        if (exit != 0) throw RuntimeException("FFmpeg falhou com código $exit")
    }

    private fun zipDirectory(sourceDir: Path, zipPath: Path) {
        ZipOutputStream(Files.newOutputStream(zipPath)).use { zos ->
            Files.walk(sourceDir).use { files ->
                files.filter { Files.isRegularFile(it) }.forEach { file ->
                    zos.putNextEntry(ZipEntry(sourceDir.relativize(file).toString()))
                    Files.copy(file, zos)
                    zos.closeEntry()
                }
            }
        }
    }

    private fun deleteTempDir(dir: Path) {
        try {
            Files.walk(dir).use { files ->
                files.sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
            }
        } catch (e: IOException) {
            log.warn("Falha ao limpar tmpdir {}: {}", dir, e.message)
        }
    }
}
