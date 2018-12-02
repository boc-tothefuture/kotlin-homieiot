package org.homieiot.mqtt

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.homieiot.Device
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.jupiter.api.Test


class TestHomieMqttClient {


    @Rule
    val environmentVariables = EnvironmentVariables()

    @Test
    fun `Test From Environment`() {
        val device = mockk<Device>(relaxed = true)
        every { device.stateTopic } returns ("/foo")
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            HomieMqttClient.fromEnv(device)
        }
        environmentVariables.set("MQTT_SERVER", "tcp://localhost:1883")
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            HomieMqttClient.fromEnv(device)
        }
        environmentVariables.clear("MQTT_SERVER")
        environmentVariables.set("MQTT_CLIENT_ID", "foo")
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            HomieMqttClient.fromEnv(device)
        }
        environmentVariables.clear("MQTT_CLIENT_ID")

        environmentVariables.set("MQTT_SERVER", "tcp://localhost:1883")
        environmentVariables.set("MQTT_CLIENT_ID", "foo")
        HomieMqttClient.fromEnv(device)

    }
}