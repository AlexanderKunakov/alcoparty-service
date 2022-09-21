package ru.buhinder.alcopartyservice.dto.response

data class MultipleEventResponse(
    val event: EventResponse,
    val isParticipant: Boolean,
)
