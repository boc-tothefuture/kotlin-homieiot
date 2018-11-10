package org.homieiot.device

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class TestHomieNumberProperties {

    @Test
    fun `Test Property Config`() {

        val publisherMock = PublisherFake()

        val property = NumberProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher, range = null)

        property.publishConfig()

        assertThat(publisherMock.messagePairs).containsAll(listOf(
                "foo/\$datatype" to "integer"
        ))
    }

    @Test
    fun `Test Data Type Projection`() {

        val publisher = PublisherFake()
        var messageReceived: Long? = null

        val property = NumberProperty(id = "foo", name = "bar", range = null, parentPublisher = publisher.publisher)
        property.subscribe { messageReceived = it.update }

        val message = 55L
        property.mqttReceived(message)
        assertThat(messageReceived).isNotNull().isEqualTo(message)

        property.update(100L)
        assertThat(publisher.messagePairs).last().isEqualTo("foo" to "100")
    }


    @Test
    fun `Test DSL Function`() {

        val nodeFake = NodeFake()
        val node = nodeFake.node()
        var numberProperty: HomieProperty<Long>? = null
        node.number(id = "number", retained = false, name = "foo", unit = "bar") {
            numberProperty = this
        }

        assertThat(numberProperty).isNotNull
        numberProperty!!.publishConfig()

        assertThat(nodeFake.publishedMessages).containsExactlyInAnyOrder(
                messageFor("homie", "device", "node", "\$properties", payload = "number"),
                messageFor("homie", "device", "node", "number", "\$name", payload = "foo"),
                messageFor("homie", "device", "node", "number", "\$retained", payload = "false"),
                messageFor("homie", "device", "node", "number", "\$settable", payload = "false"),
                messageFor("homie", "device", "node", "number", "\$datatype", payload = "integer"),
                messageFor("homie", "device", "node", "number", "\$unit", payload = "bar")
        )

    }

    @Test
    fun `Test Range`() {

        val publisherMock = PublisherFake()

        val property = NumberProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher, range = 10..15L)
        property.publishConfig()

        assertThat(publisherMock.messagePairs).containsAll(listOf(
                "foo/\$format" to "10:15"
        ))

        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            property.update(16)
        }


    }

}