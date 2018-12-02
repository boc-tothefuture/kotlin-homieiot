package org.homieiot.mqtt

import mu.KotlinLogging
import org.eclipse.paho.client.mqttv3.*
import org.homieiot.Device
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger


/**
 * Creates a Homie MQTT client for the supplied [org.homieiot.Device]
 *
 * @param [serverURI] in the format of tcp://host:port for the MQTT Broker to connect
 * @param [clientID] Supplied MQTT Client ID
 * @param [username] Optional username to connect to MQTT
 * @param [password] Optional password to connect to MQTT
 */
class HomieMqttClient(serverURI: String,
                      clientID: String,
                      private val username: String? = null,
                      private val password: String? = null,
                      private val homieRoot: String = "homie",
                      private val device: Device) {

    companion object {
        private const val QUALITY_OF_SERVICE: Int = 1

        private const val MQTT_SERVER: String = "MQTT_SERVER"
        private const val MQTT_CLIENT_ID: String = "MQTT_CLIENT_ID"
        private const val MQTT_USERNAME: String = "MQTT_USERNAME"
        private const val MQTT_PASSWORD: String = "MQTT_PASSWORD"


        /**
         * Creates a new HomieMqttClient for the [device] default environment variables
         * The environment must contain an MQTT_SERVER URI in format of tcp://host:port and an MQTT_CLIENT_ID variable
         * Additionally, the method supports extracting the MQTT_USERNAME and MQTT_PASSWORD environment variables
         *
         */
        fun fromEnv(device: Device): HomieMqttClient {
            val serverURI = System.getenv(MQTT_SERVER)
                    ?: throw IllegalArgumentException("Environment missing $MQTT_SERVER variable")
            val clientID = System.getenv(MQTT_CLIENT_ID)
                    ?: throw IllegalArgumentException("Environment missing $MQTT_CLIENT_ID variable")
            return HomieMqttClient(serverURI = serverURI,
                    clientID = clientID,
                    username = System.getenv(MQTT_USERNAME),
                    password = System.getenv(MQTT_PASSWORD),
                    device = device)
        }
    }


    private val connectLatch = AtomicInteger(1)

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
        setWill("$homieRoot/${device.stateTopic}", Device.InternalState.LOST.toString().toLowerCase().mqttPayload(), QUALITY_OF_SERVICE, true)
    }


    private fun MqttAsyncClient.connectFuture(): Future<Any> {
        val listener = ConnectListener()
        this.connect(connectOptions, listener)
        return listener.connectionFuture
    }


    /**
     * Connect client to MQTT library
     *
     * This method may only be called once and should be called when the application is ready to receive and send messages to the broker.
     * Client will automatically reconnect if necessary.
     *
     * @throws IllegalStateException If called more than once.
     * @return Future that completes after connection to broker is complete.
     */
    fun connect(): Future<Any> {
        check(connectLatch.compareAndSet(1, 0)) { "Connect may only be called once" }
        device.publisher.mqttPublisher = object : MqttPublisher {
            override fun publishMessage(message: HomieMqttMessage) {
                logger.debug { "Publishing message (${message.payload}) on topic ($homieRoot/${message.topic})" }
                client.publish("$homieRoot/${message.topic}", message.payload.mqttPayload(), QUALITY_OF_SERVICE, message.retained)
            }
        }
        return client.connectFuture()
    }

    /**
     * Disconnect MQTT Client
     *
     * This should be called during shutdown. Client may not be reconnected.
     * This function will wait up to 5 seconds to send any pending messages.
     */
    fun disconnect() {
        device.state = Device.InternalState.DISCONNECTED
        client.disconnect(5)
        client.close()
    }


    private fun subscribe() {
        settablePropertyMap.entries.forEach { (topic, property) ->
            client.subscribe("$homieRoot/$topic", QUALITY_OF_SERVICE) { _, message ->
                val value = String(message.payload, Charsets.UTF_8)
                property.mqttReceived(value)
            }
        }
        device.state = Device.InternalState.READY
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
            token.topics.forEach { logger.debug { "Message delivered on topic ($it)" } }
        }
    }


}