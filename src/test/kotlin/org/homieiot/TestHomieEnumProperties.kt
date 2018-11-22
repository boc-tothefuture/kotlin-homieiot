package org.homieiot

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
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
        val values = enumValues<TestEnum>().map { it.name }
        val map = enumValues<TestEnum>().associateBy { it.name }
        val property = EnumProperty(id = "foo", name = "bar", parentPublisher = publisher.publisher,
                enumValues = values,
                enumMap = map)

        property.publishConfig()

        assertThat(publisher.messagePairs).containsAll(listOf(
                Triple("foo" / "datatype".attr(), "enum", true),
                Triple("foo" / "format".attr(), values.joinToString(","), true)
        ))
    }


    @Test
    fun `Test Data Type Projection`() {

        val publisher = PublisherFake()
        var messageReceived: TestEnum? = null

        val values = enumValues<TestEnum>().map { it.name }.toList()
        val map = enumValues<TestEnum>().associateBy { it.name }
        val property = EnumProperty(id = "foo", name = "bar", enumValues = values, parentPublisher = publisher.publisher, enumMap = map)
        property.subscribe { messageReceived = it.update }

        val message = TestEnum.EAST
        property.mqttReceived(message.toString())
        assertThat(messageReceived).isNotNull.isEqualTo(message)

        property.update(TestEnum.WEST)
        assertThat(publisher.messagePairs).last().isEqualTo(Triple("foo", TestEnum.WEST.name, true))
    }

    @Test
    fun `Throws Exception of enum value doesn't exist`() {
        val publisher = PublisherFake()

        val values = enumValues<TestEnum>().map { it.name }.toList()
        val map = enumValues<TestEnum>().associateBy { it.name }
        val property = EnumProperty(id = "foo", name = "bar", enumValues = values, parentPublisher = publisher.publisher, enumMap = map)
        property.subscribe { it.update }

        assertThatExceptionOfType(NoSuchElementException::class.java).isThrownBy {
            property.mqttReceived("foo")
        }
    }

    @Test
    fun `Test DSL`() {

        val nodeFake = NodeFake()
        val node = nodeFake.node()
        var enumProperty: BaseProperty<TestEnum> = node.enum<TestEnum>(id = "enum", type = PropertyType.EVENT, name = "foo", unit = "bar") as BaseProperty

        assertThat(enumProperty).isNotNull

        enumProperty.publishConfig()

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