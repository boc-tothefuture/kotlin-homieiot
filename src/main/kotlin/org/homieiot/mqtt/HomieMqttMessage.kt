package org.homieiot.mqtt

internal data class HomieMqttMessage(private val topicSegments: List<String>, val payload: String, val retained: Boolean) {

    val topic = topicSegments.joinToString(separator = "/")

}