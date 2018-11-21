package org.homieiot.mqtt

import mu.KotlinLogging
import org.eclipse.paho.client.mqttv3.*
import org.homieiot.device.BaseHomieProperty
import org.homieiot.device.HomieDevice
import org.homieiot.device.HomieState
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future


class HomieMqttClient(serverURI: String,
                      clientID: String,
                      private val username: String? = null,
                      private val password: String? = null,
                      private val device: HomieDevice) {

    companion object {
        private const val QUALITY_OF_SERVICE: Int = 1
    }

    private fun String.mqttPayload() = this.toByteArray(Charsets.UTF_8)

    private val logger = KotlinLogging.logger {}

    private val settablePropertyMap = device.settablePropertyMap.mapKeys { it.key.joinToString("/") }

    private val client = MqttAsyncClient(serverURI, clientID).apply {
        setCallback(MqttCallback())
    }

    private val connectOptions = MqttConnectOptions().apply {
        isAutomaticReconnect = true
        isCleanSession = false
        this@HomieMqttClient.username?.let { userName = it }
        this@HomieMqttClient.password?.let { password = it.toCharArray() }
        setWill(device.stateTopic, HomieState.LOST.toString().toLowerCase().mqttPayload(), QUALITY_OF_SERVICE, true)
    }


    private fun MqttAsyncClient.connectFuture(): Future<Any> {
        val listener = ConnectListener()
        this.connect(connectOptions, listener)
        return listener.connectionFuture
    }


    fun connect(): Future<Any> {
        device.publisher.mqttPublisher = object : MqttPublisher {
            override fun publishMessage(message: HomieMqttMessage) {
                client.publish(message.topic, message.payload.mqttPayload(), QUALITY_OF_SERVICE, message.retained)
            }
        }
        return client.connectFuture()
    }

    fun disconnect() = client.disconnect()


    private fun subscribe() {
        settablePropertyMap.entries.forEach { (topic, property) ->
            client.subscribe(topic, QUALITY_OF_SERVICE) { _, message ->
                if (property is BaseHomieProperty) {
                    val value = String(message.payload, Charsets.UTF_8)
                    property.mqttReceived(value)
                } else {
                    logger.warn { "Received message on topic ($topic) with associated property setter" }
                }
            }

        }
    }

    private class ConnectListener : IMqttActionListener {
        internal val connectionFuture = CompletableFuture<Any>()

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            connectionFuture.completeExceptionally(exception)
        }

        override fun onSuccess(asyncActionToken: IMqttToken?) {
            connectionFuture.complete(null)
        }
    }

    private inner class MqttCallback : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String) {
            val connectType = if (reconnect) "Reconnection" else "Connection"
            logger.debug { "$connectType to $serverURI complete" }
            device.publishConfig()
            this@HomieMqttClient.subscribe()
        }

        override fun messageArrived(topic: String, message: MqttMessage) {
            logger.debug { "Message arrive on topic ($topic)" }
        }

        override fun connectionLost(cause: Throwable) {
            logger.warn(cause) { "Mqtt Connection Lost.  Reconnection in progress" }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken) {
            token.topics.forEach { topic ->
                logger.debug { "Message delivered on topic ($topic)" }
            }
        }
    }


}