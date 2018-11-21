package org.homieiot.device

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestHomieStringProperties {


    @Test
    fun `Test Property Config`() {

        val publisherMock = PublisherFake()

        val property = StringProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher)

        property.publishConfig()

        Assertions.assertThat(publisherMock.messagePairs).containsAll(listOf(
                Triple("foo" / "datatype".attr(), "string", true)
        ))
    }

    @Test
    fun `Test Data Type Projection`() {

        val publisher = PublisherFake()
        var messageReceived: String? = null

        val prop = StringProperty(id = "foo", name = "bar", parentPublisher = publisher.publisher)
        prop.subscribe { messageReceived = it.update }

        val message = "Hello, World"
        prop.mqttReceived(message)
        Assertions.assertThat(messageReceived).isNotNull().isEqualTo(message)

        publisher.messagePairs.clear()

        prop.update("baz")
        Assertions.assertThat(publisher.messagePairs).hasSize(1).last().isEqualTo(Triple("foo", "baz", true))
    }


    @Test
    fun `Test DSL`() {
        val nodeFake = NodeFake()
        val node = nodeFake.node()
        var stringProperty: HomieProperty<String>? = null
        node.string(id = "string", retained = false, name = "foo", unit = "bar") {
            stringProperty = this
        }

        assertThat(stringProperty).isNotNull
        stringProperty!!.publishConfig()

        assertThat(nodeFake.publishedMessages).containsExactlyInAnyOrder(
                messageFor("homie", "device", "node", "\$properties", payload = "string"),
                messageFor("homie", "device", "node", "string", "\$name", payload = "foo"),
                messageFor("homie", "device", "node", "string", "\$retained", payload = "false"),
                messageFor("homie", "device", "node", "string", "\$settable", payload = "false"),
                messageFor("homie", "device", "node", "string", "\$datatype", payload = "string"),
                messageFor("homie", "device", "node", "string", "\$unit", payload = "bar")
        )

    }


}