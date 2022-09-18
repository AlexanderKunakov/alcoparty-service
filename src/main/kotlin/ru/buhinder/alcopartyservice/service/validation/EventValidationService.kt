package ru.buhinder.alcopartyservice.service.validation

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.util.function.Tuple2
import ru.buhinder.alcopartyservice.config.LoggerDelegate
import ru.buhinder.alcopartyservice.controller.advice.exception.CannotJoinEventException
import ru.buhinder.alcopartyservice.controller.advice.exception.EntityCannotBeCreatedException
import ru.buhinder.alcopartyservice.controller.advice.exception.InsufficientPermissionException
import ru.buhinder.alcopartyservice.dto.EventDto
import ru.buhinder.alcopartyservice.entity.enums.EventStatus.ENDED
import ru.buhinder.alcopartyservice.repository.facade.EventDaoFacade
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class EventValidationService(
    private val eventDaoFacade: EventDaoFacade,
) {
    private val logger by LoggerDelegate()

    fun validateEventIsActive(eventId: UUID): Mono<UUID> {
        return eventDaoFacade.getById(eventId)
            .map {
                if (it.status == ENDED) {
                    throw CannotJoinEventException(
                        message = "Cannot join. Event has ended",
                        payload = mapOf("id" to eventId)
                    )
                }
                it.id!!
            }
    }

    fun validateDates(dto: EventDto): Mono<EventDto> {
        return dto.toMono()
            .map {
                val startDate = dto.startDate!!
                val endDate = dto.endDate!!
                val now = Instant.now().toEpochMilli()
                val nowMinusTwoWeeks = now.minus(Duration.ofDays(14).toMillis())
                val nowPlusTenYears = now.plus(Duration.ofDays(3652).toMillis())
                when {
                    startDate < nowMinusTwoWeeks -> throw EntityCannotBeCreatedException(
                        message = "Event start date & time must be less than 14 days before now",
                        payload = emptyMap()
                    )

                    startDate > endDate -> throw EntityCannotBeCreatedException(
                        message = "Event start date & time must be before Event end date & time",
                        payload = emptyMap()
                    )

                    endDate < now -> throw EntityCannotBeCreatedException(
                        message = "Event end date & time must be after the current date & time",
                        payload = emptyMap()
                    )

                    endDate > nowPlusTenYears -> throw EntityCannotBeCreatedException(
                        message = "Event End Date must be within 10 years",
                        payload = emptyMap()
                    )

                    else -> dto
                }
            }
    }

    fun validateIsEventCreator(eventIdAlcoholicIdTuple: Tuple2<UUID, UUID>): Mono<Tuple2<UUID, UUID>> {
        return eventDaoFacade.getById(eventIdAlcoholicIdTuple.t1)
            .filter { it.createdBy != eventIdAlcoholicIdTuple.t2 }
            .flatMap {
                Mono.error<Tuple2<UUID, UUID>>(
                    InsufficientPermissionException(
                        message = "Alcoholic with id ${eventIdAlcoholicIdTuple.t2} doesn't have permission to manage event",
                        payload = emptyMap()
                    )
                )
            }
            .switchIfEmpty { eventIdAlcoholicIdTuple.toMono() }
    }
}
