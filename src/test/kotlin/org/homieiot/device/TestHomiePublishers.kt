package org.homieiot.device

import org.assertj.core.api.Assertions
import org.homieiot.mqtt.HierarchicalHomiePublisher
import org.homieiot.mqtt.RootHomiePublisher
import org.junit.jupiter.api.Test

class TestHomiePublishers {


    @Test
    fun `Test Root Publisher`() {

        var rootPublisher = RootPublisherMock()

        rootPublisher.rootPublisher.publishMessage(payload = "bar")

        Assertions.assertThat(rootPublisher.publisherMock.publishedMessages).containsExactlyInAnyOrder(messageFor("foo", payload = "bar"))

    }

    @Test
    fun `Test One Level Hierarchy`() {

        var rootPublisher = RootPublisherMock()

        var publisher = HierarchicalHomiePublisher(rootPublisher.rootPublisher, listOf("bar"))
        publisher.publishMessage(payload = "baz")
        Assertions.assertThat(rootPublisher.publisherMock.publishedMessages).containsExactly(messageFor("foo", "bar", payload = "baz"))
    }

    @Test
    fun `Test Multi Level Hierarchy`() {

        var rootPublisher = RootPublisherMock()

        var publisherOne = HierarchicalHomiePublisher(rootPublisher.rootPublisher, listOf("bar"))
        var publisherTwo = HierarchicalHomiePublisher(publisherOne, listOf("baz"))
        publisherTwo.publishMessage(payload = "qux")
        Assertions.assertThat(rootPublisher.publisherMock.publishedMessages).containsExactly(messageFor("foo", "bar", "baz", payload = "qux"))
    }

    private class RootPublisherMock {
        internal val publisherMock = MqttPublisherMock()

        internal val rootPublisher = RootHomiePublisher(listOf("foo"))

        init {
            rootPublisher.mqttPublisher = publisherMock.mqttPublisher
        }

    }

}