package ru.buhinder.alcopartyservice.controller

import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.buhinder.alcopartyservice.dto.response.IdResponse
import ru.buhinder.alcopartyservice.service.EventPhotoService
import ru.buhinder.alcopartyservice.util.toUUID
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/alcoparty/event/{eventId}/photo")
class EventPhotoController(
    private val eventPhotoService: EventPhotoService,
) {

    @PostMapping
    fun addNewPhotosToEvent(
        @PathVariable("eventId") eventId: UUID,
        principal: Principal,
        @RequestPart(value = "photos")
        files: Flux<FilePart>,
    ): Mono<ResponseEntity<List<IdResponse>>> {
        return files.collectList()
            .flatMap { eventPhotoService.saveNewPhotos(it, eventId, principal.name.toUUID()) }
            .map { ResponseEntity.ok(it) }
    }

    @DeleteMapping("/{photoId}")
    fun deletePhoto(
        @PathVariable photoId: UUID,
        @PathVariable eventId: UUID,
        principal: Principal,
    ): Mono<Void> {
        return eventPhotoService.deletePhoto(eventId, photoId, principal.name.toUUID())
    }

    @PostMapping("/{photoId}/set-main")
    fun setPhotoMain(
        @PathVariable photoId: UUID,
        @PathVariable eventId: UUID,
        principal: Principal,
    ): Mono<Void> {
        return eventPhotoService.updateMainPhoto(eventId, photoId, principal.name.toUUID())
    }
}
