package ru.buhinder.alcopartyservice.model

import java.util.UUID
import ru.buhinder.alcopartyservice.dto.EventDto

data class EventModel(
    val eventDto: EventDto,
    val alcoholicId: UUID,
    val mainPhotoId: UUID?,
)
