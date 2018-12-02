package org.homieiot

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class TestNode {

    @Test
    fun `Test ID Format`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            Node(id = "", name = "bar", type = "baz", parentPublisher = mockk())
        }
    }

    @Test
    fun `Test duplicate property`() {
        val publisher = PublisherFake()
        val homieNode = Node(id = "foo", name = "bar", type = "baz", parentPublisher = publisher.publisher)
        val property = mockk<BaseProperty<Any>>()

        every { property.id } answers { "foo" }

        homieNode.addProperty(property) {}
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            homieNode.addProperty(property) {}
        }
    }

    @Test
    fun `Test Initial Publish`() {

        val publisher = PublisherFake()
        val homieNode = Node(id = "foo", name = "bar", type = "baz", parentPublisher = publisher.publisher)

        homieNode.publishConfig()

        assertThat(publisher.messagePairs).containsExactlyElementsOf(listOf(
                Triple("foo/\$name", "bar", true),
                Triple("foo/\$type", "baz", true),
                Triple("foo/\$properties", "", true)
        ))
    }

    @Test
    fun `Test Property Add`() {

        val publisher = PublisherFake()
        val homieNode = Node(id = "foo", name = "bar", type = "baz", parentPublisher = publisher.publisher)

        homieNode.publishConfig()

        assertThat(publisher.messagePairs).contains(Triple("foo/\$properties", "", true))

        homieNode.string(id = "hoot")
        homieNode.string(id = "qux")

        assertThat(publisher.messagePairs).contains(Triple("foo/\$properties", "hoot,qux", true))

    }


}