package org.homieiot.mqtt

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.homieiot.Device
import org.homieiot.Property
import org.homieiot.device
import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


@Testcontainers
class TestHomieMqttClient {

    @Rule
    val environmentVariables = EnvironmentVariables()

    @Container
    var mosquitto: GenericContainer<*> = GenericContainer<Nothing>("eclipse-mosquitto:1.5.4").withExposedPorts(1883)


    @Test
    fun `Test sends disconnect message before shutdown`() {
        TODO("Not Implemented")
    }

    @Test
    fun `Test MQTT Connection`() {
        val device = device(id = "foo", name = "name") {
        }
        var run: Boolean = false
        homieClient(device) {
            run = true
        }
        assertThat(run).isEqualTo(true)
    }

    @Test
    fun `Test MQTT Connection From Environment`() {
        assertThat(mosquitto.isRunning())
        val device = device(id = "foo", name = "name") { }

        environmentVariables.set("MQTT_SERVER", serverURI())
        environmentVariables.set("MQTT_CLIENT_ID", clientID())
        val client = HomieMqttClient.fromEnv(device = device)

        val connectFuture = client.connect()
        connectFuture.get(5, TimeUnit.SECONDS)
        assertThat(connectFuture.isDone).isEqualTo(true)

    }


    @Test
    fun `Test Homie Publish Update`() {
        var property: Property<String>? = null
        val device = device(id = "foo", name = "name") {
            node(id = "node", type = "type", name = "name") {
                property = string(id = "bar")
            }
        }

        homieClient(device) {
            val message = "foo"
            property!!.update(message)

            val publishedMessage = getPublishedMessage("homie/foo/node/bar").get(5, TimeUnit.SECONDS)
            assertThat(publishedMessage).isEqualTo(message)
        }
    }


    @Test
    fun `Test Mqtt Publish Update`() {
        var propertyUpdate: String? = null
        val device = device(id = "foo", name = "name") {
            node(id = "node", type = "type", name = "name") {
                string(id = "bar") {
                    subscribe { propertyUpdate = it.update }
                }
            }
        }

        val update = "foo"

        homieClient(device) {
            publishMessage("homie/foo/node/bar/set", update)
            await().atMost(5, TimeUnit.SECONDS).untilAsserted {
                assertThat(propertyUpdate).isEqualTo(update)
            }
        }
    }


    fun clientID() = java.util.UUID.randomUUID().toString()
    fun serverURI() = "tcp://${mosquitto.getContainerIpAddress()}:${mosquitto.getMappedPort(1883)}"

    fun homieClient(device: Device, init: (client: HomieMqttClient) -> Unit): Unit {
        assertThat(mosquitto.isRunning())
        val client = HomieMqttClient(serverURI = serverURI(),
                clientID = clientID(),
                device = device)
        val connectFuture = client.connect()
        connectFuture.get(5, TimeUnit.SECONDS)
        assertThat(connectFuture.isDone).isEqualTo(true)
        init(client)
        client.disconnect()
    }


    fun client() = MqttClient(serverURI(), clientID())

    fun publishMessage(topic: String, message: String) {
        client().apply {
            connect()
            publish(topic, MqttMessage(message.toByteArray()))
        }
    }

    fun getPublishedMessage(topic: String): Future<String> {
        val futureMessage = CompletableFuture<String>()

        client().apply {
            connect()
            subscribe(topic) { _, message ->
                futureMessage.complete(String(message.payload))
            }
        }
        return futureMessage
    }


}