package org.homieiot

internal fun <T : Comparable<T>> ClosedRange<T>.containsOrThrow(t: T) {
    if (!contains(t)) throw IllegalArgumentException("Supplied value ($t) for update is out of range ($this)")
}