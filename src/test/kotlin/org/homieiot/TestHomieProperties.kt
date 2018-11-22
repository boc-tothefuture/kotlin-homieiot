package org.homieiot

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.homieiot.mqtt.HomiePublisher
import org.junit.jupiter.api.Test

class TestHomieProperties {

    private class TestPropertyGroup(type: PropertyType = PropertyType.STATE, format: String? = null, unit: String? = null, publisher: HomiePublisher? = null) {
        val publisherMock = PublisherFake()

        val property: BaseProperty<Any> = object : BaseProperty<Any>(id = "foo",
                name = "bar",
                parentPublisher = publisher ?: publisherMock.publisher,
                unit = unit,
                datatype = "any",
                type = type,
                format = format) {

            override fun propertyUpdateFromString(update: String): PropertyUpdate<Any> {
                return PropertyUpdate(this, update)
            }
        }

        val messagePairs = publisherMock.messagePairs
    }


    @Test
    fun `Test ID Format`() {
        TODO("Not Implemented")
    }


    @Test
    fun `Test Topic Segments`() {
        val publisher = mockk<HomiePublisher>()
        val topicList = slot<List<String>>()
        every { publisher.topic(topicSegments = capture(topicList)) } answers { topicList.captured }

        val propertyGroup = TestPropertyGroup(publisher = publisher)

        assertThat(propertyGroup.property.topicSegments).isEqualTo(listOf("foo"))


    }

    @Test
    fun `Test Property Config`() {

        val propertyGroup = TestPropertyGroup()

        propertyGroup.property.publishConfig()

        assertThat(propertyGroup.messagePairs).containsExactlyElementsOf(listOf(
                Triple("foo/\$name", "bar", true),
                Triple("foo/\$settable", "false", true),
                Triple("foo/\$retained", "true", true),
                Triple("foo/\$datatype", "any", true)
        ))
    }

    @Test
    fun `Test Message Publication and Subscription`() {

        var messageReceived: Any? = null

        val propertyGroup = TestPropertyGroup()
        propertyGroup.property.subscribe { messageReceived = it.update }

        val message = "Hello, World"
        propertyGroup.property.mqttReceived(message)
        assertThat(messageReceived).isNotNull.isEqualTo(message)
    }


    @Test
    fun `Test Value Update`() {

        val propertyGroup = TestPropertyGroup()

        propertyGroup.property.update("baz")
        assertThat(propertyGroup.messagePairs).hasSize(1).last().isEqualTo(Triple("foo", "baz", true))
    }

    @Test
    fun `Test Settable`() {
        val propertyGroup = TestPropertyGroup()
        propertyGroup.property.publishConfig()

        assertThat(propertyGroup.messagePairs).doesNotContain(Triple("foo/\$settable", "true", true))

        propertyGroup.property.subscribe { }

        assertThat(propertyGroup.messagePairs).contains(Triple("foo/\$settable", "true", true))
    }

    @Test
    fun `Test Retained`() {
        val propertyGroup = TestPropertyGroup(type = PropertyType.EVENT)

        propertyGroup.property.publishConfig()

        assertThat(propertyGroup.messagePairs).doesNotContain(Triple("foo/\$retained", "true", true))
        assertThat(propertyGroup.messagePairs).contains(Triple("foo/\$retained", "false", true))
    }

    @Test
    fun `Test Update Only On Change`() {
        val propertyGroup = TestPropertyGroup()

        propertyGroup.property.update("baz")
        assertThat(propertyGroup.messagePairs).hasSize(1).last().isEqualTo(Triple("foo", "baz", true))
        propertyGroup.property.update("baz")
        assertThat(propertyGroup.messagePairs).hasSize(1)
    }

    @Test
    fun `Test Unit`() {
        val propertyGroup = TestPropertyGroup(unit = "foo")
        propertyGroup.property.publishConfig()
        assertThat(propertyGroup.messagePairs).contains(Triple("foo/\$unit", "foo", true))
    }

    @Test
    fun `Test Format`() {
        val propertyGroup = TestPropertyGroup(format = "foo")
        propertyGroup.property.publishConfig()
        assertThat(propertyGroup.messagePairs).contains(Triple("foo/\$format", "foo", true))
    }

}


