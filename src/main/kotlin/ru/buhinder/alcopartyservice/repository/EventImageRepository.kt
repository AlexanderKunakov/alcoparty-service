package ru.buhinder.alcopartyservice.repository

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.buhinder.alcopartyservice.entity.EventPhotoEntity
import ru.buhinder.alcopartyservice.entity.enums.PhotoType
import java.util.UUID

interface EventImageRepository : ReactiveCrudRepository<EventPhotoEntity, UUID> {

    fun findAllByEventId(eventId: UUID): Flux<EventPhotoEntity>

    fun findByEventIdAndType(eventId: UUID, type: PhotoType): Mono<EventPhotoEntity>

    fun findAllByEventIdAndTypeNot(eventId: UUID, type: PhotoType): Flux<EventPhotoEntity>

}
