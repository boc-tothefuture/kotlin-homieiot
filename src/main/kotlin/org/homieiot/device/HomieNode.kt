package org.homieiot.device

import org.homieiot.mqtt.HierarchicalHomiePublisher
import org.homieiot.mqtt.HomiePublisher


class HomieNode(val id: String, val name: String = id, val type: String, parentPublisher: HomiePublisher) : HomieUnit {

    private val _properties = mutableMapOf<String, HomieProperty<*>>()

    internal val properties: Map<String, HomieProperty<*>>
        get() = _properties

    @PublishedApi
    internal val publisher = HierarchicalHomiePublisher(parentPublisher, id)

    @PublishedApi
    internal fun <T> addProperty(property: HomieProperty<T>, init: HomieProperty<T>.() -> Unit): HomieProperty<T> {
        property.init()
        if (_properties.containsKey(property.id)) {
            throw IllegalArgumentException("Duplicate properties IDs are not allowed - duplicate id ($property.id)")
        }
        _properties[property.id] = property
        publishProperties()
        return property
    }

    internal fun publishConfig(recursive: Boolean) {
        publisher.publishMessage("name".homieAttribute(), payload = name)
        publisher.publishMessage("type".homieAttribute(), payload = type)
        publishProperties()
        if (recursive) _properties.values.forEach { it.publishConfig() }
    }

    override fun publishConfig() = publishConfig(false)


    private fun publishProperties() {
        publisher.publishMessage("properties".homieAttribute(), payload = _properties.keys.joinToString(separator = ","))
    }

    fun string(id: String,
               name: String? = null,
               retained: Boolean = true,
               unit: String? = null,
               init: ((HomieProperty<String>.() -> Unit)) = {}): HomieProperty<String> {

        return addProperty(StringProperty(id = id, name = name, retained = retained, parentPublisher = this.publisher, unit = unit), init)
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

    fun rgb(id: String,
            name: String? = null,
            retained: Boolean = true,
            unit: String? = null,
            init: ((HomieProperty<RGB>.() -> Unit)) = {}) {
        addProperty(RGBColorProperty(id = id, name = name, retained = retained, unit = unit, parentPublisher = this.publisher), init)
    }

    fun hsv(id: String,
            name: String? = null,
            retained: Boolean = true,
            unit: String? = null,
            init: ((HomieProperty<HSV>.() -> Unit)) = {}) {
        addProperty(HSVColorProperty(id = id, name = name, retained = retained, unit = unit, parentPublisher = this.publisher), init)
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

        val property = EnumProperty(id = id, name = name, retained = retained, unit = unit,
                parentPublisher = this.publisher,
                enumValues = enumValues<E>().map { it.name },
                enumMap = enumValues<E>().associateBy { it.name }
        )
        addProperty(property, init)
    }


}