package org.homieiot.device

import org.homieiot.mqtt.HierarchicalHomiePublisher
import org.homieiot.mqtt.HomiePublisher


class HomieNode(val id: String, val name: String = id, val type: String, parentPublisher: HomiePublisher) : HomieUnit {
    private val properties = arrayListOf<HomieProperty<*>>()

    private val publisher = HierarchicalHomiePublisher(parentPublisher, id)

    private fun addProperty(property: HomieProperty<*>) {
        properties += property
        publishProperties()
    }

    override fun publishConfig() {
        publisher.publishMessage("name".homieAttribute(), payload = name)
        publisher.publishMessage("type".homieAttribute(), payload = type)
        publishProperties()
    }

    private fun publishProperties() {
        publisher.publishMessage("properties".homieAttribute(), payload = properties.joinToString(separator = ",") { it.id })
    }

    fun string(id: String, name: String? = null, retained: Boolean = true, unit: String? = null) {
        addProperty(StringProperty(id = id, name = name, retained = retained, parentPublisher = this.publisher, unit = unit))
    }

    fun int(id: String, name: String? = null, retained: Boolean = true, unit: String? = null, range: IntRange? = null) {
        addProperty(IntProperty(id = id, name = name, retained = retained, unit = unit, parentPublisher = this.publisher, range = range))
    }

}