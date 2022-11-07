package ru.buhinder.alcopartyservice.entity

import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import ru.buhinder.alcopartyservice.entity.enums.EventStatus
import ru.buhinder.alcopartyservice.entity.enums.EventType
import java.time.Instant
import java.util.UUID

@Table("event")
open class EventEntity(
    id: UUID? = null,
    val title: String,
    val info: String,
    val type: EventType,
    val location: String,
    val status: EventStatus,
    val startDate: Long,
    val endDate: Long,
    private val createdAt: Long? = Instant.now().toEpochMilli(),
    val createdBy: UUID,
    @Version
    open var version: Int? = null,
    var mainPhotoId: UUID? = null,
) : AbstractAuditable(id)
