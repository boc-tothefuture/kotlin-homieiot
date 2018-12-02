package org.homieiot

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
                Triple("foo" / "datatype".attr(), "float", true)
        ))
    }

    @Test
    fun `Test Data Type Projection`() {

        val publisher = PublisherFake()
        var messageReceived: Double? = null

        val property = FloatProperty(id = "foo", name = "bar", range = null, parentPublisher = publisher.publisher)
        property.subscribe { messageReceived = it.update }

        val message = 55.0
        property.mqttReceived(message.toString())
        assertThat(messageReceived).isNotNull().isEqualTo(message)

        property.update(100.0)
        assertThat(publisher.messagePairs).last().isEqualTo(Triple("foo", "100.0", true))
    }


    @Test
    fun `Test DSL Function`() {

        val nodeFake = NodeFake()
        val node = nodeFake.node()
        var numberProperty: BaseProperty<Double> = node.float(id = "float", type = PropertyType.EVENT, name = "foo", unit = "bar") as BaseProperty

        assertThat(numberProperty).isNotNull
        numberProperty.publishConfig()

        assertThat(nodeFake.publishedMessages).containsExactlyInAnyOrder(
                messageFor("device", "node", "\$properties", payload = "float"),
                messageFor("device", "node", "float", "\$name", payload = "foo"),
                messageFor("device", "node", "float", "\$retained", payload = "false"),
                messageFor("device", "node", "float", "\$settable", payload = "false"),
                messageFor("device", "node", "float", "\$datatype", payload = "float"),
                messageFor("device", "node", "float", "\$unit", payload = "bar")
        )

    }

    @Test
    fun `Test Range`() {

        val publisherMock = PublisherFake()

        val property = FloatProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher, range = 10.0..15.0)
        property.publishConfig()

        assertThat(publisherMock.messagePairs).containsAll(listOf(
                Triple("foo" / "format".attr(), "10.0:15.0", true)
        ))

        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            property.update(16.0)
        }


    }

}