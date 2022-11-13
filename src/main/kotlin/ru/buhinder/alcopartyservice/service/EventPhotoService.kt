package ru.buhinder.alcopartyservice.service

import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.util.function.Tuples
import ru.buhinder.alcopartyservice.dto.response.IdResponse
import ru.buhinder.alcopartyservice.entity.EventEntity
import ru.buhinder.alcopartyservice.entity.EventPhotoEntity
import ru.buhinder.alcopartyservice.entity.enums.PhotoType
import ru.buhinder.alcopartyservice.repository.facade.EventDaoFacade
import ru.buhinder.alcopartyservice.repository.facade.EventPhotoDaoFacade
import ru.buhinder.alcopartyservice.service.validation.EventAlcoholicValidationService
import ru.buhinder.alcopartyservice.service.validation.PhotoValidationService
import java.util.UUID

@Service
class EventPhotoService(
    private val eventPhotoDaoFacade: EventPhotoDaoFacade,
    private val eventDaoFacade: EventDaoFacade,
    private val minioService: MinioService,
    private val eventAlcoholicValidationService: EventAlcoholicValidationService,
    private val photoValidationService: PhotoValidationService,
) {

    fun savePhotos(
        photos: List<FilePart>,
        eventId: UUID,
    ): Mono<List<EventPhotoEntity>> = minioService.savePhotos(photos)
        .map { eventPhotoEntities(it, eventId) }
        .flatMap { eventPhotoDaoFacade.saveAll(it) }

    fun savePhoto(
        id: UUID = UUID.randomUUID(),
        photo: FilePart,
        eventId: UUID,
        type: PhotoType,
    ) = minioService.savePhoto(photo)
        .map { EventPhotoEntity(id, eventId, it, type) }
        .flatMap { eventPhotoDaoFacade.save(it) }
        .map { IdResponse(eventId) }

    private fun eventPhotoEntities(photosIds: Set<UUID>, eventId: UUID): List<EventPhotoEntity> {
        return photosIds
            .map { EventPhotoEntity(eventId = eventId, photoId = it, type = PhotoType.ACTIVE) }
    }

    fun getPhotoById(id: UUID): Mono<ByteArray> {
        return eventPhotoDaoFacade.getById(id)
            .flatMap { minioService.getPhoto(it.photoId) }
    }

    @Transactional
    fun deletePhoto(eventId: UUID, photoId: UUID, alcoholicId: UUID): Mono<Void> {
        return eventDaoFacade.getById(eventId)
            .flatMap { event ->
                eventAlcoholicValidationService.validateUserIsTheEventOwner(event, alcoholicId)
                    .flatMap { eventPhotoDaoFacade.getById(photoId) }
                    .map { Tuples.of(event, it) }
            }
            .flatMap { deletePhotoFromEvent(it.t1, it.t2) }
    }

    private fun deletePhotoFromEvent(
        eventEntity: EventEntity,
        eventPhotoEntity: EventPhotoEntity,
    ): Mono<Void> {
        return when (eventPhotoEntity.type) {
            PhotoType.ACTIVE, PhotoType.ENDED -> deleteEventNotMainPhoto(eventPhotoEntity)
            PhotoType.MAIN -> deleteEventMainPhoto(eventEntity, eventPhotoEntity)
            else -> throw IllegalStateException("Photo with id ${eventPhotoEntity.id} can't be deleted")
        }
    }

    private fun deleteEventNotMainPhoto(eventPhotoEntity: EventPhotoEntity): Mono<Void> {
        return eventPhotoDaoFacade.delete(eventPhotoEntity)
    }

    private fun deleteEventMainPhoto(
        eventEntity: EventEntity,
        eventMainPhotoEntity: EventPhotoEntity,
    ): Mono<Void> {
        return eventPhotoDaoFacade.findNextMainPhotoEntity(eventEntity.id!!)
            .flatMap { newMainPhoto ->
                eventPhotoDaoFacade.changePhotoType(newMainPhoto, PhotoType.MAIN)
                    .flatMap {
                        eventPhotoDaoFacade.delete(eventMainPhotoEntity).thenReturn(it)
                    }
            }
            .flatMap { saveEventWithNewMainPhoto(eventEntity, it.id) }
            .switchIfEmpty {
                deleteMainPhotoAndSetEmptyMainPhoto(
                    eventEntity,
                    eventMainPhotoEntity
                )
            }
            .then()
    }

    private fun deleteMainPhotoAndSetEmptyMainPhoto(
        eventEntity: EventEntity,
        eventMainPhotoEntity: EventPhotoEntity,
    ): Mono<EventEntity> = saveEventWithNewMainPhoto(eventEntity, null)
        .flatMap { deleteEventNotMainPhoto(eventMainPhotoEntity).thenReturn(it) }

    private fun saveEventWithNewMainPhoto(
        eventEntity: EventEntity,
        eventPhotoId: UUID?
    ): Mono<EventEntity> = eventEntity
        .apply { mainPhotoId = eventPhotoId }
        .let { eventDaoFacade.save(it) }

    fun saveNewPhotos(
        photos: List<FilePart>,
        eventId: UUID,
        alcoholicId: UUID,
    ): Mono<List<IdResponse>> {
        return photoValidationService.validatePhotoFormat(photos)
            .flatMap {
                eventAlcoholicValidationService.validateAlcoholicIsAParticipant(
                    eventId,
                    alcoholicId
                )
            }
            .flatMap { savePhotos(photos, eventId) }
            .flatMapMany { Flux.fromIterable(it) }
            .map { IdResponse(it.id!!) }
            .collectList()
    }

    @Transactional
    fun updateMainPhoto(
        eventId: UUID,
        eventPhotoId: UUID,
        alcoholicId: UUID,
    ): Mono<Void> {
        return eventAlcoholicValidationService.validateUserIsTheEventOwner(eventId, alcoholicId)
            .flatMap {
                eventDaoFacade.getById(eventId)
                    .flatMap { updateMainPhotoForEvent(it, eventPhotoId) }
            }
            .then()
    }

    private fun updateMainPhotoForEvent(
        event: EventEntity,
        newMainPhotoId: UUID
    ): Mono<EventPhotoEntity> {
        return if (event.mainPhotoId == null) {
            saveEventWithNewMainPhoto(event, newMainPhotoId)
                .flatMap { eventPhotoDaoFacade.changePhotoTypeById(newMainPhotoId, PhotoType.MAIN) }
        } else {
            eventPhotoDaoFacade.changePhotoTypeById(event.mainPhotoId!!, PhotoType.ACTIVE)
                .flatMap { saveEventWithNewMainPhoto(event, newMainPhotoId) }
                .flatMap { eventPhotoDaoFacade.changePhotoTypeById(newMainPhotoId, PhotoType.MAIN) }
        }
    }

    fun findAllNotMainByEventId(eventId: UUID): Flux<EventPhotoEntity> {
        return eventPhotoDaoFacade.findAllByEventIdAndTypeNotMain(eventId)
    }
}