package org.homieiot.device

fun HomieMessage.asPairs(): List<Pair<String, String>> {
    return this.toHomieMessages().map { Pair(it.topic, it.payload) }
}