package org.homieiot.device

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private class TestHomieEnumProperties {

    private enum class TestEnum {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    @Test
    fun `Test Property Config`() {

        val publisher = PublisherFake()
        val values = enumValues<TestEnum>().map { it.name }.toList()
        val property = EnumProperty<TestEnum>(id = "foo", name = "bar", parentPublisher = publisher.publisher, enumValues = values)

        property.publishConfig()

        assertThat(publisher.messagePairs).containsAll(listOf(
                "foo/\$datatype" to "enum",
                "foo/\$format" to values.joinToString(",")
        ))
    }


    @Test
    fun `Test Data Type Projection`() {

        val publisher = PublisherFake()
        var messageReceived: TestEnum? = null

        val values = enumValues<TestEnum>().map { it.name }.toList()
        val property = EnumProperty<TestEnum>(id = "foo", name = "bar", enumValues = values, parentPublisher = publisher.publisher)
        property.subscribe { messageReceived = it.update }

        val message = TestEnum.EAST
        property.mqttReceived(message)
        assertThat(messageReceived).isNotNull.isEqualTo(message)

        property.update(TestEnum.WEST)
        assertThat(publisher.messagePairs).last().isEqualTo("foo" to TestEnum.WEST.name)

    }

    @Test
    fun `Test DSL`() {

        val nodeFake = NodeFake()
        val node = nodeFake.node()
        var enumProperty: HomieProperty<TestEnum>? = null
        node.enum<TestEnum>(id = "enum", retained = false, name = "foo", unit = "bar") {
            enumProperty = this

        }

        assertThat(enumProperty).isNotNull

        enumProperty!!.publishConfig()

        assertThat(nodeFake.publishedMessages).containsExactlyInAnyOrder(
                messageFor("homie", "device", "node", "\$properties", payload = "enum"),
                messageFor("homie", "device", "node", "enum", "\$name", payload = "foo"),
                messageFor("homie", "device", "node", "enum", "\$retained", payload = "false"),
                messageFor("homie", "device", "node", "enum", "\$settable", payload = "false"),
                messageFor("homie", "device", "node", "enum", "\$datatype", payload = "enum"),
                messageFor("homie", "device", "node", "enum", "\$format", payload = "NORTH,SOUTH,EAST,WEST"),
                messageFor("homie", "device", "node", "enum", "\$unit", payload = "bar")
        )

    }


}