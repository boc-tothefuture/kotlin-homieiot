package org.homieiot.mqtt

internal data class MqttMessage(private val topicSegements: List<String>, val payload: String) {

    val topic = topicSegements.joinToString(separator = "/");

}