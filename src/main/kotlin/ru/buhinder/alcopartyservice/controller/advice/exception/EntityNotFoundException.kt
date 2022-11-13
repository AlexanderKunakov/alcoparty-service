package ru.buhinder.alcopartyservice.controller.advice.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import ru.buhinder.alcopartyservice.controller.advice.dto.AlcoholicErrorCode
import ru.buhinder.alcopartyservice.controller.advice.dto.ErrorCode

class EntityNotFoundException(
    responseStatus: HttpStatus = NOT_FOUND,
    code: ErrorCode = AlcoholicErrorCode.NOT_FOUND,
    message: String,
    payload: Map<String, Any> = emptyMap(),
) : AlcoholicApiException(
    responseStatus = responseStatus,
    code = code,
    message = message,
    payload = payload
)
