package ru.buhinder.alcopartyservice.repository.facade

import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.buhinder.alcopartyservice.controller.advice.exception.EntityNotFoundException
import ru.buhinder.alcopartyservice.entity.EventEntity
import ru.buhinder.alcopartyservice.repository.EventRepository
import java.util.UUID

@Repository
class EventDaoFacade(
    private val eventRepository: EventRepository,
    private val eventImageDaoFacade: EventImageDaoFacade,
) {
    companion object {
        const val NOT_FOUND_MESSAGE = "Event not found"
    }

    fun insert(eventEntity: EventEntity): Mono<EventEntity> {
        return eventRepository.save(eventEntity)
    }

    fun deleteById(eventId: UUID): Mono<Void> {
        return eventRepository.deleteById(eventId)
    }

    fun getById(eventId: UUID): Mono<EventEntity> {
        return eventRepository.findById(eventId)
            .switchIfEmpty {
                Mono.error(
                    EntityNotFoundException(
                        message = NOT_FOUND_MESSAGE,
                        payload = mapOf("id" to eventId)
                    )
                )
            }
    }

    fun findAllAndAlcoholicIsNotBanned(alcoholicId: UUID, page: Int, pageSize: Int): Flux<EventEntity> {
        return eventRepository.findAllAndAlcoholicIsNotBanned(alcoholicId, page, pageSize)
    }

    fun countAllAndAlcoholicIsNotBanned(alcoholicId: UUID): Mono<Long> {
        return eventRepository.countAllAndAlcoholicIsNotBanned(alcoholicId)
    }

    fun findByIdAndAlcoholicIsNotBanned(eventId: UUID, alcoholicId: UUID): Mono<EventEntity> {
        return eventRepository.findByIdAndAlcoholicIsNotBanned(eventId, alcoholicId)
    }

    fun getByIdAndAlcoholicIsNotBanned(eventId: UUID, alcoholicId: UUID): Mono<EventEntity> {
        return eventRepository.findByIdAndAlcoholicIsNotBanned(eventId, alcoholicId)
            .switchIfEmpty {
                Mono.error(
                    EntityNotFoundException(
                        message = NOT_FOUND_MESSAGE,
                        payload = mapOf("id" to eventId)
                    )
                )
            }
    }

    fun getByIdAndAlcoholicIsNotBannedAndStatusNotEnded(eventId: UUID, alcoholicId: UUID): Mono<EventEntity> {
        return eventRepository.findByIdAndAlcoholicIsNotBannedAndStatusNotEnded(eventId, alcoholicId)
            .switchIfEmpty {
                Mono.error(
                    EntityNotFoundException(
                        message = NOT_FOUND_MESSAGE,
                        payload = mapOf("id" to eventId)
                    )
                )
            }
    }

    fun getByInvitationLinkAndNotEnded(invitationLink: UUID): Mono<EventEntity> {
        return eventRepository.findByInvitationLinkAndNotEnded(invitationLink)
            .switchIfEmpty {
                Mono.error(
                    EntityNotFoundException(
                        message = NOT_FOUND_MESSAGE,
                        payload = emptyMap()
                    )
                )
            }
    }

    fun findAllByAlcoholicIdAndIsNotBanned(
        alcoholicId: UUID,
        page: Int,
        pageSize: Int
    ): Flux<EventEntity> {
        return eventRepository.findAllByAlcoholicIdAndIsNotBanned(alcoholicId, page, pageSize)
    }

    fun countAllByAlcoholicIdAndIsNotBanned(alcoholicId: UUID): Mono<Long> {
        return eventRepository.countAllByAlcoholicIdAndIsNotBanned(alcoholicId)
    }

    fun findByImageId(imageId: UUID): Mono<EventEntity> {
        return eventImageDaoFacade.findById(imageId)
            .flatMap { eventRepository.findById(it.id!!) }
    }
}
