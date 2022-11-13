package ru.buhinder.alcopartyservice.controller.advice.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import ru.buhinder.alcopartyservice.controller.advice.dto.AlcoholicErrorCode.CANNOT_BE_UPDATED
import ru.buhinder.alcopartyservice.controller.advice.dto.ErrorCode

class EntityCannotBeUpdatedException(
    responseStatus: HttpStatus = CONFLICT,
    code: ErrorCode = CANNOT_BE_UPDATED,
    message: String,
    payload: Map<String, Any> = emptyMap(),
) : AlcoholicApiException(
    responseStatus = responseStatus,
    code = code,
    message = message,
    payload = payload
)
