package ru.buhinder.alcopartyservice.repository.facade

import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.buhinder.alcopartyservice.config.LoggerDelegate
import ru.buhinder.alcopartyservice.entity.EventAlcoholicEntity
import ru.buhinder.alcopartyservice.repository.EventAlcoholicRepository
import java.util.UUID

@Repository
class EventAlcoholicDaoFacade(
    private val eventAlcoholicRepository: EventAlcoholicRepository,
) {
    private val logger by LoggerDelegate()

    fun save(eventAlcoholicEntity: EventAlcoholicEntity): Mono<EventAlcoholicEntity> {
        return eventAlcoholicRepository.save(eventAlcoholicEntity)
    }

    fun update(eventAlcoholicEntity: EventAlcoholicEntity): Mono<EventAlcoholicEntity> {
        return eventAlcoholicRepository.save(eventAlcoholicEntity)
    }

    fun delete(eventAlcoholicEntity: EventAlcoholicEntity): Mono<Void> {
        return eventAlcoholicRepository.delete(eventAlcoholicEntity)
    }

    fun insertAll(alcoholics: List<EventAlcoholicEntity>): Mono<List<EventAlcoholicEntity>> {
        return eventAlcoholicRepository.saveAll(alcoholics)
            .collectList()
            .map { it.toList() }
    }

    fun findByEventIdAndAlcoholicId(eventId: UUID, alcoholicId: UUID): Mono<EventAlcoholicEntity> {
        return Mono.just(logger.info("Trying to find alcoholic with id $alcoholicId for event with id $eventId"))
            .flatMap {
                eventAlcoholicRepository.findByEventIdAndAlcoholicIdAndIsBannedIsFalse(
                    eventId,
                    alcoholicId
                )
            }
            .doOnNext { logger.info("Found alcoholic with id $alcoholicId for event with id $eventId") }
            .doOnError { logger.info("Error retrieving alcoholic with id $alcoholicId for event with id $eventId") }
    }

    fun findByEventIdAndAlcoholicIdAndIsBannedIsFalse(eventId: UUID, alcoholicId: UUID): Mono<EventAlcoholicEntity> {
        return eventAlcoholicRepository.findByEventIdAndAlcoholicIdAndIsBannedIsFalse(
            eventId = eventId,
            alcoholicId = alcoholicId
        )
    }

    fun findAllByEventId(eventId: UUID): Flux<EventAlcoholicEntity> {
        return eventAlcoholicRepository.findAllByEventId(eventId)
    }

}
