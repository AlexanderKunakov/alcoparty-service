package ru.buhinder.alcopartyservice.controller

import org.springframework.http.MediaType.IMAGE_JPEG_VALUE
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.buhinder.alcopartyservice.service.EventImageService
import ru.buhinder.alcopartyservice.util.toUUID
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/alcoparty/image")
class ImageController(
    private val eventImageService: EventImageService,
) {

    @GetMapping("/{imageId}", produces = [IMAGE_JPEG_VALUE])
    fun getImage(@PathVariable imageId: UUID): Mono<ByteArray> {
        return eventImageService.getPhotoById(imageId)
    }

    @DeleteMapping("/{imageId}")
    fun deleteImage(
        @PathVariable imageId: UUID,
        principal: Principal
    ): Mono<Void> {
        return eventImageService.deletePhotoById(imageId, principal.name.toUUID())
    }
}
