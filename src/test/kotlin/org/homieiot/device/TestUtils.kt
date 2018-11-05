package org.homieiot.device

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.homieiot.mqtt.HomiePublisher
import org.homieiot.mqtt.MqttMessage
import org.homieiot.mqtt.MqttPublisher


internal class MqttPublisherMock {

    internal val mqttPublisher = mockk<MqttPublisher>()

    internal val publishedMessages = mutableListOf<MqttMessage>()

    init {
        every {
            mqttPublisher.publishMessage(capture(publishedMessages))
        } just Runs
    }

    internal fun assertMessages(vararg messages: MqttMessage) {
        Assertions.assertThat(publishedMessages).containsExactlyInAnyOrderElementsOf(messages.toList())
    }
}

internal class PublisherFake {


    internal val messagePairs = mutableListOf<Pair<String, String>>()

    internal val publisher = object : HomiePublisher {
        override fun publishMessage(topicSegments: List<String>?, payload: String) {
            messagePairs += topicSegments!!.joinToString("/") to payload
        }

    }

}


internal fun messageFor(vararg segments: String, payload: String): MqttMessage = MqttMessage(segments.toList(), payload)

