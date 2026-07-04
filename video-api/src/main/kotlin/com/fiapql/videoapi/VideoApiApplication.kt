package com.fiapql.videoapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VideoApiApplication

fun main(args: Array<String>) {
    runApplication<VideoApiApplication>(*args)
}
