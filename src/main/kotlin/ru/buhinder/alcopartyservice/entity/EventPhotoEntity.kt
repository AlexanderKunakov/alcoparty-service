package ru.buhinder.alcopartyservice.entity

import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import ru.buhinder.alcopartyservice.entity.enums.PhotoType
import java.time.Instant
import java.util.UUID

@Table("event_photo")
open class EventPhotoEntity(
    id: UUID? = null,
    val eventId: UUID,
    val photoId: UUID,
    var type: PhotoType,
    private val createdAt: Long? = Instant.now().toEpochMilli(),
    @Version
    open var version: Int? = null,
) : AbstractAuditable(id)
