package org.homieiot.device

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestHomieNode {


    @Test
    fun `Test Initial Publish`() {

        val publisher = PublisherFake()
        val homieNode = HomieNode(id = "foo", name = "bar", type = "baz", parentPublisher = publisher.publisher)

        homieNode.publishConfig()

        Assertions.assertThat(publisher.messagePairs).containsExactlyElementsOf(listOf(
                "foo/\$name" to "bar",
                "foo/\$type" to "baz",
                "foo/\$properties" to ""
        ))
    }

    @Test
    fun `Test Property Add`() {

        val publisher = PublisherFake()
        val homieNode = HomieNode(id = "foo", name = "bar", type = "baz", parentPublisher = publisher.publisher)

        homieNode.publishConfig()

        Assertions.assertThat(publisher.messagePairs).contains("foo/\$properties" to "")

        homieNode.string(id = "hoot")
        homieNode.string(id = "qux")

        Assertions.assertThat(publisher.messagePairs).contains("foo/\$properties" to "hoot,qux")

    }


}