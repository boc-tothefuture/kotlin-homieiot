package org.homieiot.device

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class TestHomieFloatProperties {

    @Test
    fun `Test Property Config`() {

        val publisherMock = PublisherFake()

        val property = FloatProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher, range = null)

        property.publishConfig()

        assertThat(publisherMock.messagePairs).containsAll(listOf(
                "foo/\$datatype" to "float"
        ))
    }

    @Test
    fun `Test Data Type Projection`() {

        val publisher = PublisherFake()
        var messageReceived: Double? = null

        val property = FloatProperty(id = "foo", name = "bar", range = null, parentPublisher = publisher.publisher)
        property.subscribe { messageReceived = it.update }

        val message = 55.0
        property.mqttReceived(message)
        assertThat(messageReceived).isNotNull().isEqualTo(message)

        property.update(100.0)
        assertThat(publisher.messagePairs).last().isEqualTo("foo" to "100.0")
    }


    @Test
    fun `Test DSL Function`() {

        val nodeFake = NodeFake()
        val node = nodeFake.node()
        var numberProperty: HomieProperty<Double>? = null
        node.float(id = "float", retained = false, name = "foo", unit = "bar") {
            numberProperty = this
        }

        assertThat(numberProperty).isNotNull
        numberProperty!!.publishConfig()

        assertThat(nodeFake.publishedMessages).containsExactlyInAnyOrder(
                messageFor("homie", "device", "node", "\$properties", payload = "float"),
                messageFor("homie", "device", "node", "float", "\$name", payload = "foo"),
                messageFor("homie", "device", "node", "float", "\$retained", payload = "false"),
                messageFor("homie", "device", "node", "float", "\$settable", payload = "false"),
                messageFor("homie", "device", "node", "float", "\$datatype", payload = "float"),
                messageFor("homie", "device", "node", "float", "\$unit", payload = "bar")
        )

    }

    @Test
    fun `Test Range`() {

        val publisherMock = PublisherFake()

        val property = FloatProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher, range = 10.0..15.0)
        property.publishConfig()

        assertThat(publisherMock.messagePairs).containsAll(listOf(
                "foo/\$format" to "10.0:15.0"
        ))

        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            property.update(16.0)
        }


    }

}