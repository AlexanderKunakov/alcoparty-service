package ru.buhinder.alcopartyservice.controller

import org.springframework.http.MediaType.IMAGE_JPEG_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.buhinder.alcopartyservice.service.EventPhotoService
import java.util.UUID

@RestController
@RequestMapping("/api/alcoparty/photo")
class PhotoController(
    private val eventPhotoService: EventPhotoService,
) {

    @GetMapping("/{photoId}", produces = [IMAGE_JPEG_VALUE])
    fun getPhoto(@PathVariable photoId: UUID): Mono<ByteArray> {
        return eventPhotoService.getPhotoById(photoId)
    }
}
