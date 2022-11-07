package ru.buhinder.alcopartyservice.repository.facade

import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.buhinder.alcopartyservice.entity.EventPhotoEntity
import ru.buhinder.alcopartyservice.entity.enums.PhotoType
import ru.buhinder.alcopartyservice.repository.EventImageRepository
import java.util.UUID

@Repository
class EventImageDaoFacade(
    private val eventPhotoRepository: EventImageRepository,
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
        return eventPhotoRepository.findAllByEventIdAndTypeNot(eventId, PhotoType.MAIN)
    }

    fun findNextMainPhotoEntity(eventId: UUID): Mono<EventPhotoEntity> {
        return eventPhotoRepository.findFirstByEventIdAndTypeNotOrderByCreatedAtAsc(
            eventId,
            PhotoType.MAIN
        )
    }

    fun findById(imageId: UUID): Mono<EventPhotoEntity> = eventPhotoRepository.findById(imageId)

    fun delete(eventPhotoEntity: EventPhotoEntity): Mono<Void> {
        return changePhotoType(eventPhotoEntity, PhotoType.DELETED).then()
    }

    fun changePhotoType(
        eventPhotoEntity: EventPhotoEntity,
        newType: PhotoType,
    ): Mono<EventPhotoEntity> = eventPhotoEntity
        .apply { type = newType }
        .let { eventPhotoRepository.save(it) }
}
