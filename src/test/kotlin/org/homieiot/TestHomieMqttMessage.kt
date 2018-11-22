package org.homieiot

import org.assertj.core.api.Assertions
import org.homieiot.mqtt.HomieMqttMessage
import org.junit.jupiter.api.Test

class TestHomieMqttMessage {

    @Test
    fun `Test Topic String`() {


        val message = HomieMqttMessage(listOf("foo", "bar", "baz"), "", true)

        Assertions.assertThat(message.topic).isEqualTo("foo/bar/baz")
    }


}