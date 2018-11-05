package org.homieiot.device

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestHomieDevice {


    @Test
    fun `Test Initial Publish`() {


        val homieDevice = device(id = "foo", name = "foo") {
            node(id = "qux", type = "qoo") {
                string("moo")
            }
        }

        var publisherMock = MqttPublisherMock()

        homieDevice.publisher.mqttPublisher = publisherMock.mqttPublisher

        homieDevice.publishConfig()

        publisherMock.assertMessages(
                messageFor("homie", "foo", "\$state", payload = "init"),
                messageFor("homie", "foo", "\$homie", payload = "3.1.0"),
                messageFor("homie", "foo", "\$name", payload = "foo"),
                messageFor("homie", "foo", "\$implementation", payload = "kotlin-homie"),
                messageFor("homie", "foo", "\$nodes", payload = "qux")
        )
    }


    @Test
    fun `Test State Update`() {

        val homieDevice = device(id = "foo", name = "foo") { }

        var publisherMock = MqttPublisherMock()

        homieDevice.publisher.mqttPublisher = publisherMock.mqttPublisher

        Assertions.assertThat(homieDevice.state).isEqualTo(HomieState.INIT)
        homieDevice.state = HomieState.READY

        publisherMock.assertMessages(messageFor("homie", "foo", "\$state",
                payload = HomieState.READY.toString().toLowerCase()))
    }

    @Test
    fun `Test Node Add`() {

        val homieDevice = device(id = "foo", name = "foo") {
            node(id = "qux", type = "qoo") {
                string("moo")
            }
        }

        var publisherMock = MqttPublisherMock()

        homieDevice.publisher.mqttPublisher = publisherMock.mqttPublisher

        homieDevice.publishConfig()

        Assertions.assertThat(publisherMock.publishedMessages).contains(messageFor("homie", "foo", "\$nodes", payload = "qux"))

        homieDevice.node(id = "hoo", type = "owl") {
            string(id = "hoot")
        }

        Assertions.assertThat(publisherMock.publishedMessages).contains(messageFor("homie", "foo", "\$nodes", payload = "qux,hoo"))

    }


}