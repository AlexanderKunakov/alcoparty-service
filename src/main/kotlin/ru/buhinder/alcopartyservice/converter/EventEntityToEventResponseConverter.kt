package ru.buhinder.alcopartyservice.converter

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import ru.buhinder.alcopartyservice.dto.response.EventResponse
import ru.buhinder.alcopartyservice.entity.EventEntity

@Component
class EventEntityToEventResponseConverter : Converter<EventEntity, EventResponse> {

    override fun convert(source: EventEntity): EventResponse {
        return EventResponse(
            id = source.id!!,
            title = source.title,
            info = source.info,
            type = source.type,
            location = source.location,
            status = source.status,
            startDate = source.startDate,
            endDate = source.endDate,
            createdBy = source.createdBy,
            mainPhotoId = source.mainPhotoId,
        )
    }

}
