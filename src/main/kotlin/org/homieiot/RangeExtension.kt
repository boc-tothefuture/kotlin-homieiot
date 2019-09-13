package org.homieiot

internal fun <T : Comparable<T>> ClosedRange<T>.containsOrThrow(t: T) {
    require(contains(t)) { "Supplied value ($t) for update is out of range ($this)" }
}