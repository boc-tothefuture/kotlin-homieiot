package org.homieiot.mqtt

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.homieiot.MqttPublisherMock
import org.homieiot.messageFor
import org.junit.jupiter.api.Test

class TestHomiePublishers {

    @Test
    fun `Test Root Topic`() {
        assertThat(RootHomiePublisher(listOf("foo")).topic()).isEqualTo(listOf("foo"))
        assertThat(RootHomiePublisher(listOf("foo")).topic(listOf("bar"))).isEqualTo(listOf("foo", "bar"))
    }

    @Test
    fun `Test Hierarchical Topic`() {
        val rootPublisher = RootPublisherMock()
        val publisher = HierarchicalHomiePublisher(rootPublisher.rootPublisher, listOf("bar"))
        assertThat(publisher.topic()).isEqualTo(listOf("foo", "bar"))
        assertThat(publisher.topic(listOf("baz"))).isEqualTo(listOf("foo", "bar", "baz"))
    }


    @Test
    fun `Test Retained`() {
        val rootPublisher = RootPublisherMock()
        rootPublisher.rootPublisher.publishMessage(payload = "bar")

        assertThat(rootPublisher.messages).containsExactly(messageFor("foo", payload = "bar", retained = true))

        rootPublisher.messages.clear()

        rootPublisher.rootPublisher.publishMessage(payload = "bar", retained = false)
        assertThat(rootPublisher.messages).containsExactly(messageFor("foo", payload = "bar", retained = false))
    }

    @Test
    fun `Test Root Publisher`() {

        val rootPublisher = RootPublisherMock()

        rootPublisher.rootPublisher.publishMessage(payload = "bar")

        Assertions.assertThat(rootPublisher.publisherMock.publishedMessages).containsExactlyInAnyOrder(messageFor("foo", payload = "bar"))

    }

    @Test
    fun `Test One Level Hierarchy`() {

        val rootPublisher = RootPublisherMock()

        val publisher = HierarchicalHomiePublisher(rootPublisher.rootPublisher, listOf("bar"))
        publisher.publishMessage(payload = "baz")
        assertThat(rootPublisher.publisherMock.publishedMessages).containsExactly(messageFor("foo", "bar", payload = "baz"))
    }

    @Test
    fun `Test Multi Level Hierarchy`() {

        val rootPublisher = RootPublisherMock()

        val publisherOne = HierarchicalHomiePublisher(rootPublisher.rootPublisher, listOf("bar"))
        val publisherTwo = HierarchicalHomiePublisher(publisherOne, listOf("baz"))
        publisherTwo.publishMessage(payload = "qux")
        assertThat(rootPublisher.publisherMock.publishedMessages).containsExactly(messageFor("foo", "bar", "baz", payload = "qux"))
    }

    private class RootPublisherMock {
        internal val publisherMock = MqttPublisherMock()

        internal val messages = publisherMock.publishedMessages

        internal val rootPublisher = RootHomiePublisher(listOf("foo"))

        init {
            rootPublisher.mqttPublisher = publisherMock.mqttPublisher
        }

    }

}