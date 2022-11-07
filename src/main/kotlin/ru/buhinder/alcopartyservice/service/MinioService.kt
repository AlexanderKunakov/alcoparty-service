package ru.buhinder.alcopartyservice.service

import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.UploadObjectArgs
import org.springframework.http.MediaType.IMAGE_JPEG_VALUE
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import ru.buhinder.alcopartyservice.config.properties.MinioProperties
import java.io.File
import java.util.UUID
import java.util.stream.Collectors


@Service
class MinioService(
    private val minioClient: MinioClient,
    private val minioProperties: MinioProperties,
) {

    fun saveImages(images: List<FilePart>): Mono<Set<UUID>> {
        return Flux.fromIterable(images)
            .publishOn(Schedulers.boundedElastic())
            .map { saveOneImage(it) }
            .collect(Collectors.toSet())
    }

    fun saveImage(image: FilePart): Mono<UUID> {
        return image.toMono()
            .publishOn(Schedulers.boundedElastic())
            .map { saveOneImage(it) }
    }

    private fun saveOneImage(it: FilePart): UUID {
        val temp = File(it.filename())
        temp.canWrite()
        temp.canRead()
        it.transferTo(temp).block()
        val objectUUID = minioClient.uploadObject(
            UploadObjectArgs.builder()
                .`object`("${UUID.randomUUID()}")
                .filename(temp.absolutePath)
                .bucket(minioProperties.bucket)
                .contentType(IMAGE_JPEG_VALUE)
                .build()
        ).`object`()
        temp.delete()

        return UUID.fromString(objectUUID)
    }

    fun getImage(imageId: UUID): Mono<ByteArray> {
        return Mono.just(imageId)
            .map {
                minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(minioProperties.bucket)
                        .`object`("$it")
                        .build()
                )
            }
            .publishOn(Schedulers.boundedElastic())
            .map { it.readAllBytes() }
    }

    fun getImages(imagesIds: Set<UUID>): Flux<ByteArray> {
        return Flux.fromIterable(imagesIds)
            .publishOn(Schedulers.boundedElastic())
            .map {
                minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(minioProperties.bucket)
                        .`object`("$it")
                        .build()
                )
                    .readAllBytes()
            }
    }

}
