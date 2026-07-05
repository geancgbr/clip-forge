package com.fiapql.videoapi.config

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fiapql.videoapi.dto.VideoResponse
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(cf: RedisConnectionFactory): RedisCacheManager {
        // O cache "videos" guarda sempre List<VideoResponse>; o serializer tipado
        // dispensa metadados de classe e o JavaTimeModule habilita LocalDateTime
        val mapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        val listOfVideos = mapper.typeFactory
            .constructCollectionType(List::class.java, VideoResponse::class.java)
        val serializer = Jackson2JsonRedisSerializer<Any>(mapper, listOfVideos)

        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(30))          // cache de status por 30s
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )

        return RedisCacheManager.builder(cf).cacheDefaults(config).build()
    }
}
