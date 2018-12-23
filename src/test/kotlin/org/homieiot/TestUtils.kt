package org.homieiot

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.homieiot.mqtt.HomieMqttMessage
import org.homieiot.mqtt.HomiePublisher
import org.homieiot.mqtt.MqttPublisher


internal operator fun String.div(other: String) = "$this/$other"
internal fun String.attr() = "\$$this"


internal class MqttPublisherMock {

    internal val mqttPublisher = mockk<MqttPublisher>()

    internal val publishedMessages = mutableListOf<HomieMqttMessage>()

    init {
        every {
            mqttPublisher.publishMessage(capture(publishedMessages))
        } just Runs
    }

    internal fun assertContainsExactly(vararg messages: HomieMqttMessage) {
        assertThat(publishedMessages).containsExactlyInAnyOrderElementsOf(messages.toList())
    }

    internal fun assertContains(vararg messages: HomieMqttMessage) {
        assertThat(publishedMessages).containsAll(messages.toList())
    }

}

internal class PublisherFake {


    internal val messagePairs = mutableListOf<Triple<String, String, Boolean>>()

    internal val publisher = object : HomiePublisher {

        override fun topic(topicSegments: List<String>?): List<String> = topicSegments.orEmpty()

        override fun publishMessage(topicSegments: List<String>?, payload: String, retained: Boolean) {
            messagePairs += Triple(topicSegments!!.joinToString("/"), payload, retained)
        }


    }

}

internal class NodeFake {

    private val mqttMock = MqttPublisherMock()

    internal val publishedMessages = mqttMock.publishedMessages

    internal fun node(): Node {
        var node: Node? = null
        val device = device(id = "device", name = "foo") {
            node = node(id = "node", name = "bar", type = "test") {}
        }
        device.publisher.mqttPublisher = mqttMock.mqttPublisher

        return node!!
    }

}


internal fun messageFor(vararg segments: String, payload: String, retained: Boolean = true): HomieMqttMessage = HomieMqttMessage(segments.toList(), payload, retained)

