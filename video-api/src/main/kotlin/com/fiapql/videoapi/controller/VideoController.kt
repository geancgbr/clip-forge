package com.fiapql.videoapi.controller

import com.fiapql.videoapi.dto.VideoResponse
import com.fiapql.videoapi.entity.User
import com.fiapql.videoapi.service.VideoService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/videos")
class VideoController(private val videoService: VideoService) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<VideoResponse> {
        val resp = videoService.upload(file, user.id.toString(), user.email)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp)
    }

    @GetMapping
    fun list(@AuthenticationPrincipal user: User): ResponseEntity<List<VideoResponse>> =
        ResponseEntity.ok(videoService.listByUser(user.id.toString()))

    @GetMapping("/{id}")
    fun getStatus(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<VideoResponse> =
        ResponseEntity.ok(videoService.getStatus(id, user.id.toString()))
}
