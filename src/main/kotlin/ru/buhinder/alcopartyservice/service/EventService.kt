package ru.buhinder.alcopartyservice.service

import org.springframework.core.convert.ConversionService
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import ru.buhinder.alcopartyservice.controller.advice.exception.CannotJoinEventException
import ru.buhinder.alcopartyservice.dto.EventDto
import ru.buhinder.alcopartyservice.dto.response.EventResponse
import ru.buhinder.alcopartyservice.dto.response.IdResponse
import ru.buhinder.alcopartyservice.dto.response.MultipleEventResponse
import ru.buhinder.alcopartyservice.dto.response.PageableResponse
import ru.buhinder.alcopartyservice.dto.response.SingleEventResponse
import ru.buhinder.alcopartyservice.repository.facade.EventAlcoholicDaoFacade
import ru.buhinder.alcopartyservice.repository.facade.EventDaoFacade
import ru.buhinder.alcopartyservice.repository.facade.EventImageDaoFacade
import ru.buhinder.alcopartyservice.service.strategy.EventStrategyRegistry
import ru.buhinder.alcopartyservice.service.validation.EventAlcoholicValidationService
import ru.buhinder.alcopartyservice.service.validation.ImageValidationService
import java.util.UUID

@Service
class EventService(
    private val eventStrategyRegistry: EventStrategyRegistry,
    private val eventDaoFacade: EventDaoFacade,
    private val conversionService: ConversionService,
    private val eventImageDaoFacade: EventImageDaoFacade,
    private val imageValidationService: ImageValidationService,
    private val eventAlcoholicDaoFacade: EventAlcoholicDaoFacade,
    private val eventAlcoholicValidationService: EventAlcoholicValidationService,
    private val paginationService: PaginationService,
    private val eventImageService: EventImageService,
) {

    fun create(dto: EventDto, alcoholicId: UUID, images: List<FilePart>): Mono<IdResponse> {
        return imageValidationService.validateImageFormat(images)
            .flatMap { eventStrategyRegistry.get(dto.type) }
            .flatMap { it.create(dto, alcoholicId, images.firstOrNull()) }
            .flatMap { eventImageService.saveEventImages(images, it.id) }
    }

    fun join(eventId: UUID, alcoholicId: UUID): Mono<IdResponse> {
        return eventAlcoholicValidationService.validateAlcoholicIsNotBanned(eventId, alcoholicId)
            .flatMap { eventDaoFacade.getById(eventId) }
            .flatMap { eventAlcoholicValidationService.validateEventNotEnded(it) }
            .flatMap { event ->
                if (event.createdBy == alcoholicId) {
                    Mono.error(
                        CannotJoinEventException(
                            message = "You cannot join your own event",
                            payload = mapOf("id" to eventId)
                        )
                    )
                } else {
                    eventStrategyRegistry.get(event.type)
                        .flatMap { it.join(eventId = eventId, alcoholicId = alcoholicId) }
                }
            }
    }

    fun leave(eventId: UUID, alcoholicId: UUID): Mono<Void> {
        return eventAlcoholicValidationService.validateAlcoholicIsAParticipant(eventId, alcoholicId)
            .flatMap { eventAlcoholicDaoFacade.findByEventIdAndAlcoholicId(eventId, alcoholicId) }
            .flatMap { eventAlcoholicDaoFacade.delete(it) }
    }

    fun disband(eventId: UUID, currentAlcoholicId: UUID): Mono<Void> {
        return eventAlcoholicValidationService.validateUserIsTheEventOwner(eventId, currentAlcoholicId)
            .flatMap { eventDaoFacade.deleteById(eventId) }
    }

    fun getAllEvents(alcoholicId: UUID, page: Int, pageSize: Int): Mono<PageableResponse<MultipleEventResponse>> {
        return eventDaoFacade.findAllAndAlcoholicIsNotBanned(alcoholicId, page, pageSize)
            .flatMap { res ->
                val eventId = res.id!!
                eventAlcoholicDaoFacade.findAllByEventId(eventId)
                    .any { it.alcoholicId == alcoholicId }
                    .map {
                        val eventResponse =
                            conversionService.convert(res, EventResponse::class.java)!!
                        MultipleEventResponse(eventResponse, it)
                    }
            }
            .collectList()
            .switchIfEmpty { emptyList<MultipleEventResponse>().toMono() }
            .zipWith(eventDaoFacade.countAllAndAlcoholicIsNotBanned(alcoholicId))
            .map { allEventsResponse ->
                val pagination = paginationService.createPagination(allEventsResponse.t2, page, pageSize)
                PageableResponse(allEventsResponse.t1, pagination)
            }
    }

    fun getEventById(eventId: UUID, alcoholicId: UUID): Mono<SingleEventResponse> {
        return eventDaoFacade.getByIdAndAlcoholicIsNotBanned(eventId, alcoholicId)
            .map { conversionService.convert(it, EventResponse::class.java)!! }
            .flatMap { enrichEventResponseWithPhotos(it) }
    }

    fun getEventByLinkId(invitationLink: UUID): Mono<SingleEventResponse> {
        return eventDaoFacade.getByInvitationLinkAndNotEnded(invitationLink)
            .map { conversionService.convert(it, EventResponse::class.java)!! }
            .flatMap { enrichEventResponseWithPhotos(it) }
    }

    private fun enrichEventResponseWithPhotos(eventResponse: EventResponse) =
        eventImageDaoFacade.findAllByEventIdAndTypeNotMain(eventResponse.id)
            .map { it.photoId }
            .collectList()
            .flatMap { photos ->
                eventAlcoholicDaoFacade.findAllByEventId(eventResponse.id)
                    .map { it.alcoholicId }
                    .collectList()
                    .map { SingleEventResponse(eventResponse, photos, it) }
            }

    fun findAllByAlcoholicId(
        alcoholicId: UUID,
        page: Int,
        pageSize: Int
    ): Mono<PageableResponse<EventResponse>> {
        return eventDaoFacade.findAllByAlcoholicIdAndIsNotBanned(alcoholicId, page, pageSize)
            .map { conversionService.convert(it, EventResponse::class.java)!! }
            .collectList()
            .zipWith(eventDaoFacade.countAllByAlcoholicIdAndIsNotBanned(alcoholicId))
            .map { events ->
                val pagination = paginationService.createPagination(events.t2, page, pageSize)
                PageableResponse(events.t1, pagination)
            }
    }
}
