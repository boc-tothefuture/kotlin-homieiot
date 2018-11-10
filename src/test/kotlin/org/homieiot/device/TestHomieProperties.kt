package org.homieiot.device

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestHomieProperties {

    private class TestPropertyGroup(retained: Boolean = true, format: String? = null, unit: String? = null) {
        val publisherMock = PublisherFake()

        val property: BaseHomieProperty<Any> = object : BaseHomieProperty<Any>(id = "foo",
                name = "bar",
                parentPublisher = publisherMock.publisher,
                unit = unit,
                datatype = "any",
                retained = retained,
                format = format) {
        }

        val messagePairs = publisherMock.messagePairs
    }

    @Test
    fun `Test Property Config`() {

        val propertyGroup = TestPropertyGroup()

        propertyGroup.property.publishConfig()

        Assertions.assertThat(propertyGroup.messagePairs).containsExactlyElementsOf(listOf(
                "foo/\$name" to "bar",
                "foo/\$settable" to "false",
                "foo/\$retained" to "true",
                "foo/\$datatype" to "any"
        ))
    }

    @Test
    fun `Test Message Publication and Subscription`() {

        var messageReceived: Any? = null

        val propertyGroup = TestPropertyGroup()
        propertyGroup.property.subscribe { messageReceived = it.update }

        val message = "Hello, World"
        propertyGroup.property.mqttReceived(message)
        assertThat(messageReceived).isNotNull().isEqualTo(message)
    }


    @Test
    fun `Test Value Update`() {

        val propertyGroup = TestPropertyGroup()

        propertyGroup.property.update("baz")
        assertThat(propertyGroup.messagePairs).hasSize(1).last().isEqualTo("foo" to "baz")
    }

    @Test
    fun `Test Settable`() {
        val propertyGroup = TestPropertyGroup()
        propertyGroup.property.publishConfig()

        assertThat(propertyGroup.messagePairs).doesNotContain("foo/\$settable" to "true")

        propertyGroup.property.subscribe { }

        assertThat(propertyGroup.messagePairs).contains("foo/\$settable" to "true")
    }

    @Test
    fun `Test Retained`() {
        val propertyGroup = TestPropertyGroup(retained = false)

        propertyGroup.property.publishConfig()

        assertThat(propertyGroup.messagePairs).doesNotContain("foo/\$retained" to "true")
        assertThat(propertyGroup.messagePairs).contains("foo/\$retained" to "false")
    }

    @Test
    fun `Test Update Only On Change`() {
        val propertyGroup = TestPropertyGroup()

        propertyGroup.property.update("baz")
        assertThat(propertyGroup.messagePairs).hasSize(1).last().isEqualTo("foo" to "baz")
        propertyGroup.property.update("baz")
        assertThat(propertyGroup.messagePairs).hasSize(1)
    }

    @Test
    fun `Test Unit`() {
        val propertyGroup = TestPropertyGroup(unit = "foo")
        propertyGroup.property.publishConfig()
        assertThat(propertyGroup.messagePairs).contains("foo/\$unit" to "foo")
    }

    @Test
    fun `Test Format`() {
        val propertyGroup = TestPropertyGroup(format = "foo")
        propertyGroup.property.publishConfig()
        assertThat(propertyGroup.messagePairs).contains("foo/\$format" to "foo")
    }

}


