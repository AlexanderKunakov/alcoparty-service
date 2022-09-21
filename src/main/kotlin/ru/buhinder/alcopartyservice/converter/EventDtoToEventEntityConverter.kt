package ru.buhinder.alcopartyservice.converter

import java.time.Instant
import java.util.UUID
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import ru.buhinder.alcopartyservice.entity.EventEntity
import ru.buhinder.alcopartyservice.entity.enums.EventStatus
import ru.buhinder.alcopartyservice.model.EventModel

@Component
class EventDtoToEventEntityConverter : Converter<EventModel, EventEntity> {

    override fun convert(source: EventModel): EventEntity {
        val dto = source.eventDto
        val startDate = dto.startDate

        return EventEntity(
            id = UUID.randomUUID(),
            title = dto.title,
            info = dto.info,
            type = dto.type,
            location = dto.location,
            status = if (startDate!! > Instant.now().toEpochMilli()) EventStatus.SCHEDULED else EventStatus.ACTIVE,
            startDate = startDate,
            endDate = dto.endDate!!,
            createdBy = source.alcoholicId,
            mainPhotoId = source.mainPhotoId,
        )
    }

}
