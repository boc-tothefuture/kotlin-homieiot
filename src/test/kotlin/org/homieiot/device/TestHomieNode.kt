package org.homieiot.device

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestHomieNode {

    private val homieNode = HomieNode(id = "foo", name = "bar", type = "baz")

    @Test
    fun `Test MQTT Topics`() {

        val asPairs = homieNode.asPairs()

        Assertions.assertThat(asPairs).containsExactlyElementsOf(listOf(
                "foo/\$name" to "bar",
                "foo/\$type" to "baz",
                "foo/\$properties" to ""
        ))
    }


}