package org.homieiot.device

import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.homieiot.mqtt.HomiePublisher
import org.junit.jupiter.api.Test

class TestHomieProperties {

    @Test
    fun `Test String Property Without Value`() {

        val publisherMock = PublisherFake()

        val property = StringProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher)

        property.publishConfig()

        Assertions.assertThat(publisherMock.messagePairs).containsExactlyElementsOf(listOf(
                "foo/\$name" to "bar",
                "foo/\$settable" to "false",
                "foo/\$retained" to "true",
                "foo/\$datatype" to "string"
        ))
    }


    @Test
    fun `Test Message Publication`() {

        var messageReceived: String? = null

        val prop = StringProperty(id = "foo", name = "bar", observer = { messageReceived = it.update }, parentPublisher = mockk<HomiePublisher>())

        val message = "Hello, World"
        prop.mqttPublish(message)
        Assertions.assertThat(messageReceived).isNotNull().isEqualTo(message)

    }


    @Test
    fun `Test Value Update`() {

        val publisher = PublisherFake()

        val prop = StringProperty(id = "foo", name = "bar", parentPublisher = publisher.publisher)
        prop.updateValue("baz")
        Assertions.assertThat(publisher.messagePairs).hasSize(1).last().isEqualTo("foo" to "baz")
    }

}