package com.fiapql.videoapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "videos")
class Video(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false)
    var userId: String = "",

    @Column(nullable = false)
    var originalFilename: String = "",

    @Column(nullable = false)
    var minioKey: String = "",

    var zipKey: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VideoStatus = VideoStatus.PENDING,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    var updatedAt: LocalDateTime? = null
) {
    @PrePersist
    fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = createdAt
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
