package org.homieiot.device

import org.homieiot.mqtt.HierarchicalHomiePublisher
import org.homieiot.mqtt.HomiePublisher


class HomieNode(val id: String, val name: String = id, val type: String, parentPublisher: HomiePublisher) : HomieUnit {
    private val properties = arrayListOf<HomieProperty<*>>()

    @PublishedApi
    internal val publisher = HierarchicalHomiePublisher(parentPublisher, id)

    @PublishedApi
    internal fun <T> addProperty(property: HomieProperty<T>, init: HomieProperty<T>.() -> Unit): HomieProperty<T> {
        property.init()
        properties += property
        publishProperties()
        return property
    }

    override fun publishConfig() {
        publisher.publishMessage("name".homieAttribute(), payload = name)
        publisher.publishMessage("type".homieAttribute(), payload = type)
        publishProperties()
    }

    private fun publishProperties() {
        publisher.publishMessage("properties".homieAttribute(), payload = properties.joinToString(separator = ",") { it.id })
    }

    fun string(id: String,
               name: String? = null,
               retained: Boolean = true,
               unit: String? = null,
               init: ((HomieProperty<String>.() -> Unit)) = {}) {

        addProperty(StringProperty(id = id, name = name, retained = retained, parentPublisher = this.publisher, unit = unit), init)
    }


    fun number(id: String,
               name: String? = null,
               retained: Boolean = true,
               unit: String? = null,
               range: LongRange? = null,
               init: ((HomieProperty<Long>.() -> Unit)) = {}) {
        addProperty(NumberProperty(id = id, name = name, retained = retained, unit = unit, parentPublisher = this.publisher, range = range), init)
    }

    fun float(id: String,
              name: String? = null,
              retained: Boolean = true,
              unit: String? = null,
              range: ClosedFloatingPointRange<Double>? = null,
              init: ((HomieProperty<Double>.() -> Unit)) = {}) {
        addProperty(FloatProperty(id = id, name = name, retained = retained, unit = unit, parentPublisher = this.publisher, range = range), init)
    }


    fun bool(id: String,
             name: String? = null,
             retained: Boolean = true,
             unit: String? = null,
             init: ((HomieProperty<Boolean>.() -> Unit)) = {}) {
        addProperty(BoolProperty(id = id, name = name, retained = retained, unit = unit, parentPublisher = this.publisher), init)
    }

    inline fun <reified E : Enum<E>> enum(id: String,
                                          name: String? = null,
                                          retained: Boolean = true,
                                          unit: String? = null,
                                          noinline init: ((HomieProperty<E>.() -> Unit)) = {}) {
        val values = enumValues<E>().map { it.name }.toList()
        val property = EnumProperty<E>(id = id, name = name, retained = retained, unit = unit, parentPublisher = this.publisher, enumValues = values)
        addProperty(property, init)
    }


}