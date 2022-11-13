package ru.buhinder.alcopartyservice.service.strategy

import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import ru.buhinder.alcopartyservice.dto.EventDto
import ru.buhinder.alcopartyservice.dto.response.EventResponse
import ru.buhinder.alcopartyservice.dto.response.IdResponse
import ru.buhinder.alcopartyservice.entity.EventAlcoholicEntity
import ru.buhinder.alcopartyservice.entity.enums.EventType
import ru.buhinder.alcopartyservice.entity.enums.EventType.PUBLIC
import ru.buhinder.alcopartyservice.entity.enums.PhotoType
import ru.buhinder.alcopartyservice.repository.facade.EventAlcoholicDaoFacade
import ru.buhinder.alcopartyservice.service.EventPhotoService
import ru.buhinder.alcopartyservice.service.validation.EventAlcoholicValidationService
import ru.buhinder.alcopartyservice.service.validation.EventValidationService
import java.util.UUID

@Component
class PublicEventStrategy(
    private val eventCreatorDelegate: EventCreatorDelegate,
    private val eventAlcoholicDaoFacade: EventAlcoholicDaoFacade,
    private val eventAlcoholicValidationService: EventAlcoholicValidationService,
    private val eventValidationService: EventValidationService,
    private val eventPhotoService: EventPhotoService,
) : EventStrategy {

    override fun create(
        dto: EventDto,
        alcoholicId: UUID,
        mainPhoto: FilePart?
    ): Mono<EventResponse> {
        return mainPhoto?.let {
            val mainPhotoId = UUID.randomUUID()
            eventCreatorDelegate.create(dto, alcoholicId, mainPhotoId)
                .flatMap { response ->
                    eventPhotoService.savePhoto(mainPhotoId, mainPhoto, response.id, PhotoType.MAIN)
                        .map { response }
                }
        } ?: eventCreatorDelegate.create(dto, alcoholicId, null)
    }

    override fun join(eventId: UUID, alcoholicId: UUID): Mono<IdResponse> {
        return eventAlcoholicValidationService.validateAlcoholicIsNotAlreadyParticipating(
            eventId,
            alcoholicId
        )
            .flatMap { eventValidationService.validateEventIsActive(eventId) }
            .flatMap {
                eventAlcoholicDaoFacade.save(
                    EventAlcoholicEntity(
                        UUID.randomUUID(),
                        eventId,
                        alcoholicId
                    )
                )
            }
            .map { IdResponse(it.eventId) }
    }

    override fun getEventType(): EventType {
        return PUBLIC
    }

}
