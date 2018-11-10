package org.homieiot.mqtt

import mu.KotlinLogging


interface HomiePublisher {

    // fun publishMessage(message: MqttMessage)
    fun publishMessage(topicSegments: List<String>? = null, payload: String)

    fun publishMessage(topicSegment: String, payload: String) {
        publishMessage(listOf(topicSegment), payload)
    }
}

internal class HierarchicalHomiePublisher(private val parent: HomiePublisher, private val topicParts: List<String>) : HomiePublisher {

    constructor(parent: HomiePublisher, topicSegment: String) : this(parent, listOf(topicSegment))

    override fun publishMessage(topicSegments: List<String>?, payload: String) {
        parent.publishMessage(this.topicParts + topicSegments.orEmpty(), payload)
    }
}

internal class RootHomiePublisher(private val topicParts: List<String>) : HomiePublisher {

    val logger = KotlinLogging.logger {}

    var mqttPublisher: MqttPublisher = object : MqttPublisher {
        override fun publishMessage(message: MqttMessage) {
            logger.warn { "Message (${message.payload}) published on topic (${message.topic}) without connected MqttClient" }
        }
    }

    override fun publishMessage(topicSegments: List<String>?, payload: String) {
        mqttPublisher.publishMessage(MqttMessage(this.topicParts + topicSegments.orEmpty(), payload))
    }
}

internal interface MqttPublisher {
    fun publishMessage(message: MqttMessage)
}
