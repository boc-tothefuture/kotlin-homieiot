package org.homieiot

import org.homieiot.colors.HSV
import org.homieiot.colors.RGB
import org.homieiot.mqtt.HierarchicalHomiePublisher
import org.homieiot.mqtt.HomiePublisher


/**
 *
 * Nodes are independent or logically separable parts of a device. For example, a car might expose a wheels node, an engine node and a lights node.
 *
 */
class Node internal constructor(internal val id: String, private val name: String = id, private val type: String, parentPublisher: HomiePublisher) {

    /**
     * Mutable map of node properties
     */
    private val _properties = mutableMapOf<String, BaseProperty<*>>()

    /**
     * Exposed read-only view of node properties
     */
    internal val properties: Map<String, BaseProperty<*>>
        get() = _properties

    @PublishedApi
    internal val publisher = HierarchicalHomiePublisher(parentPublisher, id)

    /* Internal because of reified enum below */
    @PublishedApi
    internal fun <T> addProperty(property: BaseProperty<T>, init: Property<T>.() -> Unit): Property<T> {
        property.init()
        if (_properties.containsKey(property.id)) {
            throw IllegalArgumentException("Duplicate properties IDs are not allowed - duplicate id ($property.id)")
        }
        _properties[property.id] = property
        publishProperties()
        return property
    }

    internal fun publishConfig(includeProperties: Boolean) {
        publisher.publishMessage("name".homieAttribute(), payload = name)
        publisher.publishMessage("type".homieAttribute(), payload = type)
        publishProperties()
        if (includeProperties) _properties.values.forEach { it.publishConfig() }
    }

    internal fun publishConfig() = publishConfig(false)


    private fun publishProperties() {
        publisher.publishMessage("properties".homieAttribute(), payload = _properties.keys.joinToString(separator = ","))
    }


    /**
     * Add a property of type string to this node
     *
     * @param id Each property of a node must have a unique [id] which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids)
     * @param name Friendly name of the property, defaults to the supplied [id]
     * @param type Specifies the [org.homieiot.device.PropertyType] of property that is modeled
     * @param unit Unit of this property, [homie convention specifies some recommended units](https://homieiot.github.io/specification/spec-core-v3_0_1/#property-attributes)
     * @param init Block to embed [org.homieiot.device.Property.subscribe] and [org.homieiot.device.Property.update] functions for this property
     */
    fun string(id: String,
               name: String? = null,
               type: PropertyType = PropertyType.STATE,
               unit: String? = null,
               init: ((Property<String>.() -> Unit)) = {}): Property<String> {

        return addProperty(StringProperty(id = id, name = name, type = type, parentPublisher = this.publisher, unit = unit), init)
    }

    /**
     * Add a property of type number to this node
     *
     * @param id Each property of a node must have a unique [id] which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids)
     * @param name Friendly name of the property, defaults to the supplied [id]
     * @param type Specifies the [org.homieiot.device.PropertyType] of property that is modeled
     * @param range Specifies a range of acceptable values
     * @param unit Unit of this property, [homie convention specifies some recommended units](https://homieiot.github.io/specification/spec-core-v3_0_1/#property-attributes)
     * @param init Block to embed [org.homieiot.device.Property.subscribe] and [org.homieiot.device.Property.update] functions for this property
     */
    fun number(id: String,
               name: String? = null,
               type: PropertyType = PropertyType.STATE,
               unit: String? = null,
               range: LongRange? = null,
               init: ((Property<Long>.() -> Unit)) = {}): Property<Long> {
        return addProperty(NumberProperty(id = id, name = name, type = type, unit = unit, parentPublisher = this.publisher, range = range), init)
    }

    /**
     * Add a property of type float to this node
     *
     * @param id Each property of a node must have a unique [id] which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids)
     * @param name Friendly name of the property, defaults to the supplied [id]
     * @param type Specifies the [org.homieiot.device.PropertyType] of property that is modeled
     * @param range Specifies a range of acceptable values
     * @param unit Unit of this property, [homie convention specifies some recommended units](https://homieiot.github.io/specification/spec-core-v3_0_1/#property-attributes)
     * @param init Block to embed [org.homieiot.device.Property.subscribe] and [org.homieiot.device.Property.update] functions for this property
     */
    fun float(id: String,
              name: String? = null,
              type: PropertyType = PropertyType.STATE,
              unit: String? = null,
              range: ClosedFloatingPointRange<Double>? = null,
              init: ((Property<Double>.() -> Unit)) = {}): Property<Double> {
        return addProperty(FloatProperty(id = id, name = name, type = type, unit = unit, parentPublisher = this.publisher, range = range), init)
    }

    /**
     * Add a property of color type rgb to this node
     *
     * @param id Each property of a node must have a unique [id] which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids)
     * @param name Friendly name of the property, defaults to the supplied [id]
     * @param type Specifies the [org.homieiot.device.PropertyType] of property that is modeled
     * @param init Block to embed [org.homieiot.device.Property.subscribe] and [org.homieiot.device.Property.update] functions for this property
     */
    fun rgb(id: String,
            name: String? = null,
            type: PropertyType = PropertyType.STATE,
            init: ((Property<RGB>.() -> Unit)) = {}): Property<RGB> {
        return addProperty(RGBColorProperty(id = id, name = name, type = type, parentPublisher = this.publisher), init)
    }

    /**
     * Add a property of color type hsv to this node
     *
     * @param id Each property of a node must have a unique [id] which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids)
     * @param name Friendly name of the property, defaults to the supplied [id]
     * @param type Specifies the [org.homieiot.device.PropertyType] of property that is modeled
     * @param init Block to embed [org.homieiot.device.Property.subscribe] and [org.homieiot.device.Property.update] functions for this property
     */
    fun hsv(id: String,
            name: String? = null,
            type: PropertyType = PropertyType.STATE,
            init: ((Property<HSV>.() -> Unit)) = {}): Property<HSV> {
        return addProperty(HSVColorProperty(id = id, name = name, type = type, parentPublisher = this.publisher), init)
    }

    /**
     * Add a property of type boolean to this node
     *
     * @param id Each property of a node must have a unique [id] which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids)
     * @param name Friendly name of the property, defaults to the supplied [id]
     * @param type Specifies the [org.homieiot.device.PropertyType] of property that is modeled
     * @param init Block to embed [org.homieiot.device.Property.subscribe] and [org.homieiot.device.Property.update] functions for this property
     */
    fun bool(id: String,
             name: String? = null,
             type: PropertyType = PropertyType.STATE,
             init: ((Property<Boolean>.() -> Unit)) = {}): Property<Boolean> {
        return addProperty(BoolProperty(id = id, name = name, type = type, parentPublisher = this.publisher), init)
    }

    /**
     * Add a property of type enum to this node
     *
     * @param id Each property of a node must have a unique [id] which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids)
     * @param name Friendly name of the property, defaults to the supplied [id]
     * @param type Specifies the [org.homieiot.device.PropertyType] of property that is modeled
     * @param unit Unit of this property, [homie convention specifies some recommended units](https://homieiot.github.io/specification/spec-core-v3_0_1/#property-attributes)
     * @param init Block to embed [org.homieiot.device.Property.subscribe] and [org.homieiot.device.Property.update] functions for this property
     */
    inline fun <reified E : Enum<E>> enum(id: String,
                                          name: String? = null,
                                          type: PropertyType = PropertyType.STATE,
                                          unit: String? = null,
                                          noinline init: ((Property<E>.() -> Unit)) = {}): Property<E> {

        val property = enum(id = id, name = name, type = type, unit = unit,
                parentPublisher = this.publisher,
                enumValues = enumValues<E>().map { it.name },
                enumMap = enumValues<E>().associateBy { it.name }
        ) as BaseProperty

        return addProperty(property, init)
    }


}