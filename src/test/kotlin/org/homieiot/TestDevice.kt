package org.homieiot

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.homieiot.mqtt.HomiePublisher
import org.junit.jupiter.api.Test

class TestDevice {

    @Test
    fun `Test ID Format`() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            Device(id = "", name = "bar")
        }
    }

    @Test
    fun `Test Publish Config`() {

        val homieDevice = device(id = "foo", name = "foo") {
            node(id = "qux", type = "qoo") {
                string("moo")
            }
        }

        val publisherMock = MqttPublisherMock()

        homieDevice.publisher.mqttPublisher = publisherMock.mqttPublisher

        homieDevice.publishConfig()

        publisherMock.assertContainsExactly(
            messageFor("foo", "\$state", payload = "init"),
            messageFor("foo", "\$homie", payload = "3.0.1"),
            messageFor("foo", "\$name", payload = "foo"),
            messageFor("foo", "\$implementation", payload = "kotlin-homie"),
            messageFor("foo", "\$nodes", payload = "qux")
        )
    }

    @Test
    fun `Test Duplicate Nodes`() {

        val homieDevice = device(id = "foo", name = "foo") {
            node(id = "foo", type = "qoo") {
            }
        }
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            homieDevice.node(id = "foo", type = "goo") {}
        }
    }

    @Test
    fun `Test Node Range`() {
        var invokeCount = 0
        val range = 1..6
        val homieDevice = device(id = "foo", name = "foo") {
            node(id = "qux", type = "qoo", range = range) {
                invokeCount++
            }
        }
        assertThat(invokeCount).isEqualTo(range.last)

        val publisherMock = MqttPublisherMock()

        homieDevice.publisher.mqttPublisher = publisherMock.mqttPublisher

        homieDevice.publishConfig(includeNodes = false)

        publisherMock.assertContains(
            messageFor("foo", "nodes".attr(), payload = "qux1,qux2,qux3,qux4,qux5,qux6")
        )
    }

    @Test
    fun `Test Recursive Publish Config`() {

        val homieDevice = device(id = "foo", name = "foo") {
            node(id = "qux", type = "qoo") {
                string("moo")
            }
        }

        val publisherMock = MqttPublisherMock()

        homieDevice.publisher.mqttPublisher = publisherMock.mqttPublisher

        homieDevice.publishConfig(includeNodes = true)

        publisherMock.assertContainsExactly(
            messageFor("foo", "state".attr(), payload = "init"),
            messageFor("foo", "homie".attr(), payload = "3.0.1"),
            messageFor("foo", "name".attr(), payload = "foo"),
            messageFor("foo", "implementation".attr(), payload = "kotlin-homie"),
            messageFor("foo", "nodes".attr(), payload = "qux"),
            messageFor("foo", "qux", "name".attr(), payload = "qux"),
            messageFor("foo", "qux", "type".attr(), payload = "qoo"),
            messageFor("foo", "qux", "properties".attr(), payload = "moo"),
            messageFor("foo", "qux", "moo", "settable".attr(), payload = "false"),
            messageFor("foo", "qux", "moo", "retained".attr(), payload = "true"),
            messageFor("foo", "qux", "moo", "datatype".attr(), payload = "string")
        )
    }

    @Test
    fun `Test Settable Topic Map`() {
        val publisher: HomiePublisher = mockk()
        val slot = slot<List<String>>()
        every {
            publisher.topic(topicSegments = capture(slot))
        } answers {
            listOf("foo", "qux") + slot.captured
        }

        every {
            publisher.publishMessage(any<List<String>>(), any(), any())
        } just runs

        val moo = StringProperty(id = "moo", name = "bar", parentPublisher = publisher)
        moo.subscribe { }
        val meow = StringProperty(id = "meow", name = "bar", parentPublisher = publisher)
        val homieDevice = device(id = "foo", name = "foo") {
            val node = node(id = "qux", type = "qoo") { }
            node.addProperty(moo) {}
            node.addProperty(meow) {}
        }

        val topicMap = homieDevice.settablePropertyMap
        assertThat(topicMap).containsEntry(listOf("foo", "qux", "moo", "set"), moo)
        assertThat(topicMap).doesNotContainValue(meow)
    }

    @Test
    fun `Test State Topic`() {
        val homieDevice = device(id = "foo", name = "bar") {}
        assertThat(homieDevice.stateTopic).isEqualTo("foo/\$state")
    }

    @Test
    fun `Test State Update`() {

        val homieDevice = device(id = "foo", name = "foo") { }

        val publisherMock = MqttPublisherMock()

        homieDevice.publisher.mqttPublisher = publisherMock.mqttPublisher

        homieDevice.state(Device.State.READY)

        publisherMock.assertContainsExactly(
            messageFor(
                "foo", "\$state",
                payload = Device.InternalState.READY.toString()
            )
        )
    }

    @Test
    fun `Test Node Add`() {

        val homieDevice = device(id = "foo", name = "foo") {
            node(id = "qux", type = "qoo") {
                string("moo")
            }
        }

        val publisherMock = MqttPublisherMock()

        homieDevice.publisher.mqttPublisher = publisherMock.mqttPublisher

        homieDevice.publishConfig()

        assertThat(publisherMock.publishedMessages).contains(messageFor("foo", "\$nodes", payload = "qux"))

        homieDevice.node(id = "hoo", type = "owl") {
            string(id = "hoot")
        }

        assertThat(publisherMock.publishedMessages).contains(messageFor("foo", "\$nodes", payload = "qux,hoo"))
    }
}