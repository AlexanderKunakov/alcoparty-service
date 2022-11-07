package ru.buhinder.alcopartyservice.service

import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.util.function.Tuples
import ru.buhinder.alcopartyservice.dto.response.IdResponse
import ru.buhinder.alcopartyservice.entity.EventEntity
import ru.buhinder.alcopartyservice.entity.EventPhotoEntity
import ru.buhinder.alcopartyservice.entity.enums.PhotoType
import ru.buhinder.alcopartyservice.repository.facade.EventDaoFacade
import ru.buhinder.alcopartyservice.repository.facade.EventImageDaoFacade
import ru.buhinder.alcopartyservice.service.validation.EventAlcoholicValidationService
import ru.buhinder.alcopartyservice.util.removeFirst
import java.util.UUID

@Service
class EventImageService(
    private val eventImageDaoFacade: EventImageDaoFacade,
    private val eventDaoFacade: EventDaoFacade,
    private val minioService: MinioService,
    private val eventAlcoholicValidationService: EventAlcoholicValidationService,
) {

    fun saveEventImages(
        images: List<FilePart>,
        eventId: UUID
    ) = minioService.saveImages(images.removeFirst())
        .map { buildPhotosList(it, eventId) }
        .flatMap { eventImageDaoFacade.saveAll(it) }
        .map { IdResponse(eventId) }

    private fun buildPhotosList(photosIds: Set<UUID>, eventId: UUID): List<EventPhotoEntity> {
        return photosIds
            .map { EventPhotoEntity(eventId = eventId, photoId = it, type = PhotoType.ACTIVE) }
    }

    fun getPhotoById(id: UUID): Mono<ByteArray> {
        return eventImageDaoFacade.findById(id)
            .flatMap { minioService.getImage(it.photoId) }
    }

    @Transactional
    fun deletePhotoById(imageId: UUID, alcoholicId: UUID): Mono<Void> {
        return eventDaoFacade.findByImageId(imageId)
            .flatMap { event ->
                eventAlcoholicValidationService.validateUserIsTheEventOwner(event, alcoholicId)
                    .flatMap { eventImageDaoFacade.findById(imageId) }
                    .map { Tuples.of(event, it) }
            }
            .flatMap { deletePhotoFromEvent(it.t1, it.t2) }
    }

    private fun deletePhotoFromEvent(
        eventEntity: EventEntity,
        eventPhotoEntity: EventPhotoEntity,
    ): Mono<Void> {
        return when (eventPhotoEntity.type) {
            PhotoType.ACTIVE, PhotoType.ENDED -> deleteNotMainPhoto(eventPhotoEntity)
            PhotoType.MAIN -> deleteMainPhoto(eventEntity, eventPhotoEntity)
            else -> throw IllegalStateException("Photo with id ${eventPhotoEntity.id} can't be deleted")
        }
    }

    private fun deleteNotMainPhoto(eventPhotoEntity: EventPhotoEntity): Mono<Void> {
        return eventImageDaoFacade.delete(eventPhotoEntity)
    }

    private fun deleteMainPhoto(
        eventEntity: EventEntity,
        eventPhotoEntity: EventPhotoEntity,
    ): Mono<Void> {
        return eventImageDaoFacade.findNextMainPhotoEntity(eventEntity.id!!)
            .flatMap { eventImageDaoFacade.changePhotoType(it, PhotoType.MAIN) }
            .flatMap { eventImageDaoFacade.changePhotoType(eventPhotoEntity, PhotoType.DELETED) }
            .flatMap { setNewMainPhoto(eventEntity, it) }
            .then()
            .switchIfEmpty { deleteNotMainPhoto(eventPhotoEntity) }
    }

    private fun setNewMainPhoto(
        eventEntity: EventEntity,
        it: EventPhotoEntity
    ): Mono<EventEntity> = eventEntity
        .apply { mainPhotoId = it.id!! }
        .let { eventDaoFacade.save(it) }
        .map { it }
}