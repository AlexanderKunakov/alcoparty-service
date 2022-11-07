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
import ru.buhinder.alcopartyservice.repository.facade.EventAlcoholicDaoFacade
import ru.buhinder.alcopartyservice.service.MinioService
import ru.buhinder.alcopartyservice.service.validation.EventAlcoholicValidationService
import ru.buhinder.alcopartyservice.service.validation.EventValidationService
import java.util.UUID

@Component
class PublicEventStrategy(
    private val eventCreatorDelegate: EventCreatorDelegate,
    private val eventAlcoholicDaoFacade: EventAlcoholicDaoFacade,
    private val eventAlcoholicValidationService: EventAlcoholicValidationService,
    private val eventValidationService: EventValidationService,
    private val minioService: MinioService,
) : EventStrategy {

    override fun create(dto: EventDto, alcoholicId: UUID, mainImage: FilePart?): Mono<EventResponse> {
        return if (mainImage != null) {
            minioService.saveImage(mainImage)
                .flatMap { eventCreatorDelegate.create(dto, alcoholicId, it) }
        } else {
            eventCreatorDelegate.create(dto, alcoholicId, null)
        }
    }

    override fun join(eventId: UUID, alcoholicId: UUID): Mono<IdResponse> {
        return eventAlcoholicValidationService.validateAlcoholicIsNotAlreadyParticipating(eventId, alcoholicId)
            .flatMap { eventValidationService.validateEventIsActive(eventId) }
            .flatMap { eventAlcoholicDaoFacade.insert(EventAlcoholicEntity(UUID.randomUUID(), eventId, alcoholicId)) }
            .map { IdResponse(it.eventId) }
    }

    override fun getEventType(): EventType {
        return PUBLIC
    }

}
