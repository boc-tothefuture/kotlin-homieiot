package org.homieiot.device

import org.homieiot.mqtt.MqttMessage


@DslMarker
annotation class DeviceTagMarker


interface HomieMessage {
    fun toHomieMessages(): List<MqttMessage>
}

@DeviceTagMarker
class HomieDevice(val id: String, val name: String) : HomieMessage {
    private val nodes = arrayListOf<HomieNode>()

    fun node(id: String, type: String, name: String = id, init: HomieNode.() -> Unit): HomieNode {
        val node = HomieNode(id = id, type = type, name = name)
        node.init()
        nodes.add(node)
        return node
    }

    override fun toHomieMessages(): List<MqttMessage> {
        return listOf(
                MqttMessage(id, "\$name", payload = name)
        )
    }
}


fun device(id: String, name: String, init: HomieDevice.() -> Unit): HomieDevice {
    val device = HomieDevice(id, name)
    device.init()
    return device
}