package ru.buhinder.alcopartyservice.controller

import org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import ru.buhinder.alcopartyservice.config.properties.PaginationProperties
import ru.buhinder.alcopartyservice.dto.EventDto
import ru.buhinder.alcopartyservice.dto.InvitationLinksResponse
import ru.buhinder.alcopartyservice.dto.response.EventResponse
import ru.buhinder.alcopartyservice.dto.response.IdResponse
import ru.buhinder.alcopartyservice.dto.response.MultipleEventResponse
import ru.buhinder.alcopartyservice.dto.response.PageableResponse
import ru.buhinder.alcopartyservice.dto.response.SingleEventResponse
import ru.buhinder.alcopartyservice.service.EventAlcoholicService
import ru.buhinder.alcopartyservice.service.EventService
import ru.buhinder.alcopartyservice.service.InvitationLinkService
import java.security.Principal
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.Min

@Validated
@RestController
@RequestMapping("/api/alcoparty/event")
class EventController(
    private val eventService: EventService,
    private val eventAlcoholicService: EventAlcoholicService,
    private val paginationProperties: PaginationProperties,
    private val linkService: InvitationLinkService,
) {

    @PostMapping(consumes = [MULTIPART_FORM_DATA_VALUE])
    fun save(
        @Valid
        @RequestPart(value = "event")
        dto: EventDto,
        principal: Principal,
        @RequestPart(value = "photos", required = false)
        files: Flux<FilePart>,
    ): Mono<ResponseEntity<IdResponse>> {
        return files
            .collectList()
            .flatMap { eventService.create(dto, UUID.fromString(principal.name), it.toList()) }
            .map { ResponseEntity.ok(it) }
    }

    @PutMapping("/join/{eventId}")
    fun join(@PathVariable eventId: UUID, principal: Principal): Mono<ResponseEntity<IdResponse>> {
        return eventService.join(eventId = eventId, alcoholicId = UUID.fromString(principal.name))
            .map { ResponseEntity.ok(it) }
    }

    @PutMapping("/leave/{eventId}")
    fun leave(@PathVariable eventId: UUID, principal: Principal): Mono<ResponseEntity<Void>> {
        return eventService.leave(eventId, UUID.fromString(principal.name))
            .map { ResponseEntity.ok().build() }
    }

    @PutMapping("/disband/{eventId}")
    fun disband(@PathVariable eventId: UUID, principal: Principal): Mono<ResponseEntity<Void>> {
        return eventService.disband(eventId, UUID.fromString(principal.name))
            .map { ResponseEntity.ok().build() }
    }

    @PutMapping("/block")
    fun block(
        @RequestParam("eventId") eventId: UUID,
        @RequestParam("userId") alcoholicId: UUID,
        principal: Principal,
    ): Mono<Boolean> {
        return eventAlcoholicService.block(eventId, alcoholicId, UUID.fromString(principal.name))
    }

    @GetMapping
    fun getAllEvents(
        @Valid @Min(0) @RequestParam("page") page: Int?,
        @Valid @Min(1) @RequestParam("pageSize") pageSize: Int?,
        principal: Principal,
    ): Mono<PageableResponse<MultipleEventResponse>> {
        return eventService.getAllEvents(
            UUID.fromString(principal.name),
            page ?: paginationProperties.page,
            pageSize ?: paginationProperties.pageSize
        )
    }

    @GetMapping("/{eventId}")
    fun getEventById(@PathVariable eventId: UUID, principal: Principal): Mono<ResponseEntity<SingleEventResponse>> {
        return eventService.getEventById(eventId, UUID.fromString(principal.name))
            .map { ResponseEntity.ok(it) }
    }

    @GetMapping("/own")
    fun getOwnEvents(
        @Valid @Min(0) @RequestParam("page") page: Int?,
        @Valid @Min(1) @RequestParam("pageSize") pageSize: Int?,
        principal: Principal,
    ): Mono<ResponseEntity<PageableResponse<EventResponse>>> {
        return eventService.findAllByAlcoholicId(
            UUID.fromString(principal.name),
            page ?: paginationProperties.page,
            pageSize ?: paginationProperties.pageSize
        )
            .map { ResponseEntity.ok(it) }
    }

    @GetMapping("/link/{invitationLink}")
    fun getEventByLinkId(@PathVariable invitationLink: UUID): Mono<ResponseEntity<SingleEventResponse>> {
        return eventService.getEventByLinkId(invitationLink)
            .map { ResponseEntity.ok(it) }
    }

    @GetMapping("/{eventId}/link")
    fun getEventLinksByEventId(
        @PathVariable eventId: UUID,
        principal: Principal,
    ): Mono<InvitationLinksResponse> {
        return linkService.getInvitationLinksByEventId(
            eventId,
            UUID.fromString(principal.name),
        )
    }
}
