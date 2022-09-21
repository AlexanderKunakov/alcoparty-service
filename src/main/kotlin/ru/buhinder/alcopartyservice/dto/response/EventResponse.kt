package ru.buhinder.alcopartyservice.dto.response

import java.util.UUID
import ru.buhinder.alcopartyservice.entity.enums.EventStatus
import ru.buhinder.alcopartyservice.entity.enums.EventType

data class EventResponse(
    val id: UUID,
    val title: String,
    val info: String,
    val type: EventType,
    val location: String,
    val status: EventStatus,
    val startDate: Long,
    val endDate: Long,
    val createdBy: UUID,
    val mainPhotoId: UUID?,
)
