package org.homieiot.mqtt

import mu.KotlinLogging


interface HomiePublisher {

    fun topic(topicSegments: List<String>?): List<String>

    fun topic() = topic(listOf())

    fun publishMessage(topicSegments: List<String>? = null, payload: String, retained: Boolean = true)

    fun publishMessage(topicSegment: String, payload: String, retained: Boolean = true) {
        publishMessage(listOf(topicSegment), payload, retained)
    }
}

internal class HierarchicalHomiePublisher(private val parent: HomiePublisher, private val topicParts: List<String>) : HomiePublisher {

    constructor(parent: HomiePublisher, topicSegment: String) : this(parent, listOf(topicSegment))

    override fun topic(topicSegments: List<String>?) = parent.topic(this.topicParts + topicSegments.orEmpty())

    override fun publishMessage(topicSegments: List<String>?, payload: String, retained: Boolean) {
        parent.publishMessage(this.topicParts + topicSegments.orEmpty(), payload, retained)
    }
}

internal class RootHomiePublisher(private val topicParts: List<String>) : HomiePublisher {

    val logger = KotlinLogging.logger {}

    var mqttPublisher: MqttPublisher = object : MqttPublisher {
        override fun publishMessage(message: HomieMqttMessage) {
            logger.warn { "Message (${message.payload}) published on topic (${message.topic}) without connected MqttClient" }
        }
    }

    override fun topic(topicSegments: List<String>?) = (this.topicParts + topicSegments.orEmpty())

    override fun publishMessage(topicSegments: List<String>?, payload: String, retained: Boolean) {
        mqttPublisher.publishMessage(HomieMqttMessage(topicSegments = this.topicParts + topicSegments.orEmpty(),
                payload = payload,
                retained = retained))
    }
}

internal interface MqttPublisher {
    fun publishMessage(message: HomieMqttMessage)
}
