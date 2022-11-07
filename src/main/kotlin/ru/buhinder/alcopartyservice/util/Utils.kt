package ru.buhinder.alcopartyservice.util

import java.util.UUID

fun <E> List<E>.removeFirst(): List<E> {
    return if (isEmpty()) {
        emptyList()
    } else {
        subList(1, size)
    }
}

fun String.toUUID(): UUID {
    return UUID.fromString(this)
}
