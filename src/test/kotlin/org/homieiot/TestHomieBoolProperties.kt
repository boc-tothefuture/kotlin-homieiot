package org.homieiot

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestHomieBoolProperties {

    @Test
    fun `Test Property Config`() {

        val publisherMock = PublisherFake()

        val property = BoolProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher)

        property.publishConfig()

        Assertions.assertThat(publisherMock.messagePairs).containsAll(listOf(
                Triple("foo" / "datatype".attr(), "boolean", true)
        ))
    }


    @Test
    fun `Test Data Type Projection`() {

        val publisherMock = PublisherFake()
        var messageReceived: Boolean? = null

        val property = BoolProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher)
        property.subscribe { messageReceived = it.update }


        val message = true
        property.mqttReceived(message.toString())
        Assertions.assertThat(messageReceived).isNotNull().isEqualTo(message)
        publisherMock.messagePairs.clear()

        property.update(true)
        Assertions.assertThat(publisherMock.messagePairs).hasSize(1).last().isEqualTo(Triple("foo", "true", true))
        publisherMock.messagePairs.clear()
        property.update(false)
        Assertions.assertThat(publisherMock.messagePairs).hasSize(1).last().isEqualTo(Triple("foo", "false", true))
    }


    @Test
    fun `Test DSL Function`() {

        val nodeFake = NodeFake()
        val node = nodeFake.node()
        var boolProperty: BaseProperty<Boolean> = node.bool(id = "bool", type = PropertyType.EVENT, name = "foo") as BaseProperty<Boolean>

        Assertions.assertThat(boolProperty).isNotNull

        boolProperty.publishConfig()

        Assertions.assertThat(nodeFake.publishedMessages).containsExactlyInAnyOrder(
                messageFor("device", "node", "\$properties", payload = "bool"),
                messageFor("device", "node", "bool", "\$name", payload = "foo"),
                messageFor("device", "node", "bool", "\$retained", payload = "false"),
                messageFor("device", "node", "bool", "\$settable", payload = "false"),
                messageFor("device", "node", "bool", "\$datatype", payload = "boolean")
        )


    }

}