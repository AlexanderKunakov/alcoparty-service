package ru.buhinder.alcopartyservice.service.strategy

import java.util.UUID
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import ru.buhinder.alcopartyservice.dto.EventDto
import ru.buhinder.alcopartyservice.dto.response.EventResponse
import ru.buhinder.alcopartyservice.entity.EventAlcoholicEntity
import ru.buhinder.alcopartyservice.entity.EventEntity
import ru.buhinder.alcopartyservice.entity.EventPhotoEntity
import ru.buhinder.alcopartyservice.entity.enums.PhotoType
import ru.buhinder.alcopartyservice.model.EventModel
import ru.buhinder.alcopartyservice.repository.facade.EventAlcoholicDaoFacade
import ru.buhinder.alcopartyservice.repository.facade.EventDaoFacade
import ru.buhinder.alcopartyservice.repository.facade.EventPhotoDaoFacade
import ru.buhinder.alcopartyservice.service.validation.EventValidationService

@Component
class EventCreatorDelegate(
    private val eventDaoFacade: EventDaoFacade,
    private val eventAlcoholicDaoFacade: EventAlcoholicDaoFacade,
    private val eventValidationService: EventValidationService,
    private val conversionService: ConversionService,
    private val eventPhotoDaoFacade: EventPhotoDaoFacade,
) {
    fun create(dto: EventDto, alcoholicId: UUID, mainImageId: UUID?): Mono<EventResponse> {
        return dto.toMono()
            .flatMap { eventValidationService.validateDates(dto) }
            .map { conversionService.convert(EventModel(it, alcoholicId, mainImageId), EventEntity::class.java)!! }
            .flatMap { eventEntity ->
                eventDaoFacade.insert(eventEntity)
                    .flatMap { event ->
                        if (event.mainPhotoId != null) {
                            eventPhotoDaoFacade.insert(
                                EventPhotoEntity(
                                    eventId = event.id!!,
                                    photoId = event.mainPhotoId,
                                    type = PhotoType.MAIN
                                )
                            ).map { it }
                        } else {
                            event.toMono()
                        }
                    }
                    .map { buildAlcoholicsList(dto.alcoholicsIds, alcoholicId, eventEntity.id!!) }
                    .flatMap { eventAlcoholicDaoFacade.insertAll(it) }
                    .map { eventEntity }
            }
            .map { conversionService.convert(it, EventResponse::class.java)!! }
    }

    private fun buildAlcoholicsList(
        alcoholicsIds: Set<UUID>,
        alcoholicId: UUID,
        entityId: UUID,
    ): List<EventAlcoholicEntity> {
        return alcoholicsIds
            .plus(alcoholicId)
            .map { EventAlcoholicEntity(eventId = entityId, alcoholicId = it) }
    }

}
