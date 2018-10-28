package org.homieiot.mqtt

class MqttMessage(vararg topicSegements: String, val payload: String) {

    val topic = topicSegements.joinToString(separator = "/");

}