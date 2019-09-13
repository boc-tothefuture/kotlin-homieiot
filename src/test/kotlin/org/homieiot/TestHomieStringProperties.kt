package org.homieiot

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestHomieStringProperties {

    @Test
    fun `Test Property Config`() {

        val publisherMock = PublisherFake()

        val property = StringProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher)

        property.publishConfig()

        Assertions.assertThat(publisherMock.messagePairs).containsAll(
            listOf(
                Triple("foo" / "datatype".attr(), "string", true)
            )
        )
    }

    @Test
    fun `Test Data Type Projection`() {

        val publisher = PublisherFake()
        var messageReceived: String? = null

        val prop = StringProperty(id = "foo", name = "bar", parentPublisher = publisher.publisher)
        prop.subscribe { messageReceived = it.update }

        val message = "Hello, World"
        prop.mqttReceived(message)
        assertThat(messageReceived).isNotNull().isEqualTo(message)

        publisher.messagePairs.clear()

        prop.update("baz")
        Assertions.assertThat(publisher.messagePairs).hasSize(1).last().isEqualTo(Triple("foo", "baz", true))
    }

    @Test
    fun `Test DSL`() {
        val nodeFake = NodeFake()
        val node = nodeFake.node()
        val stringProperty: BaseProperty<String> =
            node.string(id = "string", type = PropertyType.EVENT, name = "foo", unit = "bar") as BaseProperty<String>

        assertThat(stringProperty).isNotNull
        stringProperty.publishConfig()

        assertThat(nodeFake.publishedMessages).containsExactlyInAnyOrder(
            messageFor("device", "node", "\$properties", payload = "string"),
            messageFor("device", "node", "string", "\$name", payload = "foo"),
            messageFor("device", "node", "string", "\$retained", payload = "false"),
            messageFor("device", "node", "string", "\$settable", payload = "false"),
            messageFor("device", "node", "string", "\$datatype", payload = "string"),
            messageFor("device", "node", "string", "\$unit", payload = "bar")
        )
    }
}