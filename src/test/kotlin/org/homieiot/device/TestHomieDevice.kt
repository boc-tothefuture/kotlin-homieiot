package org.homieiot.device

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestHomieDevice {

    private val homieDevice = device(id = "foo", name = "foo") { }

    @Test
    fun `Test MQTT Topics`() {

        val asPairs = homieDevice.asPairs()

        assertThat(asPairs).containsExactlyElementsOf(listOf(
                "foo/\$name" to "foo"
        ))
    }
}