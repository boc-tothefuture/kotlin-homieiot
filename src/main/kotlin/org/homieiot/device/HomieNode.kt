package org.homieiot.device


import org.homieiot.mqtt.MqttMessage

class HomieNode(val id: String, val name: String = id, val type: String) : HomieMessage {
    private val properties = arrayListOf<HomieProperty<*>>()
    override fun toHomieMessages(): List<MqttMessage> {

        return listOf(
                MqttMessage(id, "\$name", payload = name),
                MqttMessage(id, "\$type", payload = type),
                MqttMessage(id, "\$properties", payload = properties.joinToString(separator = ",") { it.id })
        )
    }

    fun string(id: String, name: String? = null, settable: Boolean = false, unit: String? = null, value: String?) = properties.add(StringProperty(id, name, settable, unit, value))

    fun int(id: String, name: String? = null, settable: Boolean = false, unit: String? = null, range: IntRange? = null, value: Int?) = properties.add(IntProperty(id, name, settable, unit, range, value))

}