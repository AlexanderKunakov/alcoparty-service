package ru.buhinder.alcopartyservice.util


fun <E> List<E>.removeFirst(): List<E> {
    return if (isEmpty()) {
        emptyList()
    } else {
        subList(1, size)
    }
}