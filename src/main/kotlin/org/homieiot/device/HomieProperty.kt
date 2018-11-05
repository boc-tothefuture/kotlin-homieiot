package org.homieiot.device

import org.homieiot.mqtt.HierarchicalHomiePublisher
import org.homieiot.mqtt.HomiePublisher

interface HomieProperty<T> : org.homieiot.device.HomieUnit {
    val id: String
    val name: String?
    val settable: Boolean
    val retained: Boolean
    val unit: String?
    val datatype: String?
    val format: String?

    fun updateValue(t: T)
}


data class PropertyUpdate<T>(val property: HomieProperty<T>, val update: T)


abstract class BaseHomieProperty<T>(override val id: String,
                                    override val name: String?,
                                    private val observer: ((PropertyUpdate<T>) -> Unit)?,
                                    parentPublisher: HomiePublisher,
                                    override val settable: Boolean,
                                    override val retained: Boolean = true,
                                    override val unit: String?,
                                    override val datatype: String,
                                    override val format: String?) : HomieProperty<T>, org.homieiot.device.HomieUnit {

    private val publisher = HierarchicalHomiePublisher(parentPublisher, id)


    override fun updateValue(t: T) {
        publisher.publishMessage(payload = valueToString(t))
    }

    protected fun valueToString(value: T): String {
        return value.toString()
    }

    internal fun mqttPublish(t: T) {
        observer?.let { it.invoke(PropertyUpdate(this, t)) }
    }

    override fun publishConfig() {
        name?.let { publisher.publishMessage("\$name", payload = it) }
        publisher.publishMessage("\$settable", payload = settable.toString())
        publisher.publishMessage("\$retained", payload = retained.toString())
        unit?.let { publisher.publishMessage("\$unit", payload = it) }
        publisher.publishMessage("\$datatype", payload = datatype)
        format?.let { publisher.publishMessage("\$format", payload = it) }
    }
}

typealias StringPropertyUpdate = PropertyUpdate<String>

class StringProperty(id: String,
                     name: String? = null,
                     observer: ((StringPropertyUpdate) -> Unit)? = null,
                     parentPublisher: HomiePublisher,
                     retained: Boolean = true,
                     unit: String? = null) : BaseHomieProperty<String>(
        id = id,
        name = name,
        observer = observer,
        settable = (observer != null),
        unit = unit,
        retained = retained,
        datatype = "string",
        parentPublisher = parentPublisher,
        format = null) {
}

typealias IntPropertyUpdate = PropertyUpdate<Int>

class IntProperty(id: String,
                  name: String?,
                  observer: ((IntPropertyUpdate) -> Unit)? = null,
                  parentPublisher: HomiePublisher,
                  retained: Boolean = true,
                  unit: String?,
                  range: IntRange?) : BaseHomieProperty<Int>(
        id = id,
        name = name,
        observer = observer,
        settable = (observer != null),
        retained = retained,
        unit = unit,
        parentPublisher = parentPublisher,
        datatype = "integer",
        format = range?.let { "${it.first}:${it.last}" }
)