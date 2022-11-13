package ru.buhinder.alcopartyservice.dto.response

import java.util.UUID

data class SingleEventResponse(
    val event: EventResponse,
    val photos: List<UUID>,
    val participants: List<UUID>,
)
