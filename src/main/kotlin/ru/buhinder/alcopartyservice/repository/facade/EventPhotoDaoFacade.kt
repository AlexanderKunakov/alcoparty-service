package ru.buhinder.alcopartyservice.repository.facade

import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.buhinder.alcopartyservice.controller.advice.exception.EntityNotFoundException
import ru.buhinder.alcopartyservice.entity.EventPhotoEntity
import ru.buhinder.alcopartyservice.entity.enums.PhotoType
import ru.buhinder.alcopartyservice.repository.EventPhotoRepository
import java.util.UUID

@Repository
class EventPhotoDaoFacade(
    private val eventPhotoRepository: EventPhotoRepository,
) {
    fun save(eventPhotoEntity: EventPhotoEntity): Mono<EventPhotoEntity> {
        return eventPhotoRepository.save(eventPhotoEntity)
    }

    fun saveAll(photos: List<EventPhotoEntity>): Mono<List<EventPhotoEntity>> {
        return eventPhotoRepository.saveAll(photos)
            .collectList()
            .map { it.toList() }
    }

    fun findAllByEventIdAndTypeNotMain(eventId: UUID): Flux<EventPhotoEntity> {
        return eventPhotoRepository.findAllByEventIdAndTypeNotIn(
            eventId,
            listOf(PhotoType.MAIN, PhotoType.DELETED)
        )
    }

    fun findNextMainPhotoEntity(eventId: UUID): Mono<EventPhotoEntity> {
        return eventPhotoRepository.findFirstByEventIdAndTypeNotInOrderByCreatedAtAsc(
            eventId,
            listOf(PhotoType.MAIN, PhotoType.DELETED)
        )
    }

    fun getById(id: UUID): Mono<EventPhotoEntity> {
        return eventPhotoRepository.findById(id)
            .switchIfEmpty {
                Mono.error { EntityNotFoundException(message = "Photo with id $id not found") }
            }
    }

    fun delete(eventPhotoEntity: EventPhotoEntity): Mono<Void> {
        return changePhotoType(eventPhotoEntity, PhotoType.DELETED).then()
    }

    fun changePhotoTypeById(
        id: UUID,
        newType: PhotoType,
    ): Mono<EventPhotoEntity> {
        return getById(id)
            .flatMap { changePhotoType(it, newType) }
    }

    fun changePhotoType(
        eventPhotoEntity: EventPhotoEntity,
        newType: PhotoType,
    ): Mono<EventPhotoEntity> = eventPhotoEntity
        .apply { type = newType }
        .let { eventPhotoRepository.save(it) }
}
