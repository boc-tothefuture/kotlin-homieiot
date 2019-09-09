package org.homieiot

import org.homieiot.colors.HSV
import org.homieiot.colors.RGB
import org.homieiot.mqtt.HierarchicalHomiePublisher
import org.homieiot.mqtt.HomiePublisher

/**
 * A [Homie Property](https://homieiot.github.io/specification/#properties)
 *
 * @property id Unique ID that conforms to [Homie convention for IDs](https://homieiot.github.io/specification/#topic-ids)
 * @property name User supplied friendly name for property
 */
interface Property<T> {

    val id: String
    val name: String?

    /**
     * Submits an update to controller
     * @param t Updated value
     */
    fun update(t: T)

    /**
     * Called when the controller sends a property update
     * @param update Update received from controller
     */
    fun subscribe(update: (PropertyUpdate<T>) -> Unit)
}


/**
 * A property update delivered from MQTT
 *
 * @param [property] Property that was updated
 * @param update Update received from MQTT
 */
data class PropertyUpdate<T> internal constructor(val property: Property<T>, val update: T)

/**
 * Defines the different types of properties depending on the feature of the node being modeled.
 */
enum class PropertyType {
    /**
     * [STATE] types are useful for properties that represent persistent values (temperature sensor, light switch), etc.
     */
    STATE,

    /**
     * [EVENT] types are be useful for properties that represent momentary events (door bell pressed).
     */
    EVENT
}

internal abstract class BaseProperty<T>(final override val id: String,
                                        final override val name: String?,
                                        parentPublisher: HomiePublisher,
                                        private val type: PropertyType,
                                        val unit: String?,
                                        val datatype: String,
                                        val format: String?) : Property<T> {


    init {
        idRequire(id)
    }

    private val publisher = HierarchicalHomiePublisher(parentPublisher, id)

    private var observer: ((PropertyUpdate<T>) -> Unit)? = null

    private var lastValue: T? = null

    internal val settable: Boolean
        get() = observer != null

    internal val topicSegments = publisher.topic()

    internal val retained = type == PropertyType.STATE


    override fun update(t: T) {
        if (type == PropertyType.EVENT || t != lastValue) {
            publisher.publishMessage(payload = valueToString(t))
            lastValue = t
        }
    }

    protected open fun valueToString(value: T) = value.toString()

    internal fun mqttReceived(update: String) {
        observer?.invoke(propertyUpdateFromString(update))
    }

    abstract fun propertyUpdateFromString(update: String): PropertyUpdate<T>


    internal fun publishConfig() {
        name?.let { publisher.publishMessage("\$name", payload = it) }
        publishSettable()
        publisher.publishMessage("\$retained", payload = retained.toString())
        unit?.let { publisher.publishMessage("\$unit", payload = it) }
        publisher.publishMessage("\$datatype", payload = datatype)
        format?.let { publisher.publishMessage("\$format", payload = it) }
    }

    private fun publishSettable() {
        publisher.publishMessage("\$settable", payload = settable.toString())
    }

    override fun subscribe(update: (PropertyUpdate<T>) -> Unit) {
        this.observer = update
        publishSettable()
    }

}


internal class StringProperty(id: String,
                              name: String? = null,
                              parentPublisher: HomiePublisher,
                              type: PropertyType = PropertyType.STATE,
                              unit: String? = null) : BaseProperty<String>(
        id = id,
        name = name,
        unit = unit,
        type = type,
        datatype = "string",
        parentPublisher = parentPublisher,
        format = null) {

    override fun propertyUpdateFromString(update: String): PropertyUpdate<String> = PropertyUpdate(this, update)
}


internal abstract class AbstractNumberProperty<T : Comparable<T>>(id: String,
                                                                  name: String?,
                                                                  parentPublisher: HomiePublisher,
                                                                  type: PropertyType = PropertyType.STATE,
                                                                  datatype: String,
                                                                  unit: String? = null,
                                                                  private val range: ClosedRange<T>?) : BaseProperty<T>(
        id = id,
        name = name,
        parentPublisher = parentPublisher,
        type = type,
        unit = unit,
        datatype = datatype,
        format = range?.let { "${it.start}:${it.endInclusive}" }) {

    override fun update(t: T) {
        range?.containsOrThrow(t)
        super.update(t)
    }
}


internal class NumberProperty(id: String,
                              name: String?,
                              parentPublisher: HomiePublisher,
                              type: PropertyType = PropertyType.STATE,
                              unit: String? = null,
                              range: LongRange?) : AbstractNumberProperty<Long>(
        id = id,
        name = name,
        type = type,
        unit = unit,
        parentPublisher = parentPublisher,
        datatype = "integer",
        range = range) {

    override fun propertyUpdateFromString(update: String): PropertyUpdate<Long> = PropertyUpdate(this, update.toLong())
}


internal class FloatProperty(id: String,
                             name: String?,
                             parentPublisher: HomiePublisher,
                             type: PropertyType = PropertyType.STATE,
                             unit: String? = null,
                             range: ClosedRange<Double>?) : AbstractNumberProperty<Double>(

        id = id,
        name = name,
        type = type,
        unit = unit,
        parentPublisher = parentPublisher,
        datatype = "float",
        range = range) {

    override fun propertyUpdateFromString(update: String): PropertyUpdate<Double> = PropertyUpdate(this, update.toDouble())
}


internal class EnumProperty<E : Enum<E>>(id: String,
                                         name: String?,
                                         parentPublisher: HomiePublisher,
                                         type: PropertyType = PropertyType.STATE,
                                         unit: String? = null,
                                         enumValues: List<String>,
                                         private val enumMap: Map<String, E>
) : BaseProperty<E>(
        id = id,
        name = name,
        type = type,
        unit = unit,
        parentPublisher = parentPublisher,
        datatype = "enum",
        format = enumValues.joinToString(",")
) {

    override fun propertyUpdateFromString(update: String): PropertyUpdate<E> {
        return PropertyUpdate(this, enumMap.getValue(update))
    }
}


internal class BoolProperty(id: String,
                            name: String?,
                            parentPublisher: HomiePublisher,
                            type: PropertyType = PropertyType.STATE) : BaseProperty<Boolean>(
        id = id,
        name = name,
        type = type,
        parentPublisher = parentPublisher,
        unit = null,
        datatype = "boolean",
        format = null) {

    override fun propertyUpdateFromString(update: String): PropertyUpdate<Boolean> = PropertyUpdate(this, update.toBoolean())
}


internal abstract class AbstractColorProperty<T> internal constructor(id: String,
                                                                      name: String?,
                                                                      parentPublisher: HomiePublisher,
                                                                      type: PropertyType = PropertyType.STATE,
                                                                      colorType: String) : BaseProperty<T>(
        id = id,
        name = name,
        type = type,
        parentPublisher = parentPublisher,
        datatype = "color",
        unit = null,
        format = colorType) {

    protected fun parseColorString(string: String): Triple<Int, Int, Int> {
        val (first, second, third) = string.split(',').map { it.toInt() }
        return Triple(first, second, third)
    }


}


internal class HSVColorProperty(id: String,
                                name: String?,
                                parentPublisher: HomiePublisher,
                                type: PropertyType = PropertyType.STATE) : AbstractColorProperty<HSV>(
        id = id,
        name = name,
        type = type,
        parentPublisher = parentPublisher,
        colorType = "hsv") {

    override fun propertyUpdateFromString(update: String): PropertyUpdate<HSV> = PropertyUpdate(this, HSV(parseColorString(update)))


    override fun valueToString(value: HSV): String = "${value.hue},${value.saturation},${value.value}"

}

internal class RGBColorProperty(id: String,
                                name: String?,
                                parentPublisher: HomiePublisher,
                                type: PropertyType = PropertyType.STATE) : AbstractColorProperty<RGB>(
        id = id,
        name = name,
        type = type,
        parentPublisher = parentPublisher,
        colorType = "rgb") {

    override fun valueToString(value: RGB): String = "${value.red},${value.green},${value.blue}"

    override fun propertyUpdateFromString(update: String): PropertyUpdate<RGB> = PropertyUpdate(this, RGB(parseColorString(update)))
}

