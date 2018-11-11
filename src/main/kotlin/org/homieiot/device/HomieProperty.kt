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

    fun update(t: T)
    fun subscribe(update: (PropertyUpdate<T>) -> Unit): HomieProperty<T>
}


data class PropertyUpdate<T>(val property: HomieProperty<T>, val update: T)


abstract class BaseHomieProperty<T>(override val id: String,
                                    override val name: String?,
                                    parentPublisher: HomiePublisher,
                                    override val retained: Boolean,
                                    override val unit: String?,
                                    override val datatype: String,
                                    override val format: String?) : HomieProperty<T>, org.homieiot.device.HomieUnit {

    private val publisher = HierarchicalHomiePublisher(parentPublisher, id)

    private var observer: ((PropertyUpdate<T>) -> Unit)? = null

    private var lastValue: T? = null

    override val settable: Boolean
        get() = observer != null


    override fun update(t: T) {
        if (t != lastValue) {
            publisher.publishMessage(payload = valueToString(t))
            lastValue = t
        }
    }

    protected open fun valueToString(value: T): String {
        return value.toString()
    }

    internal fun mqttReceived(t: T) {
        observer?.invoke(PropertyUpdate(this, t))
    }

    override fun publishConfig() {
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

    override fun subscribe(update: (PropertyUpdate<T>) -> Unit): HomieProperty<T> {
        this.observer = update
        publishSettable()
        return this
    }

}


class StringProperty(id: String,
                     name: String? = null,
                     parentPublisher: HomiePublisher,
                     retained: Boolean = true,
                     unit: String? = null) : BaseHomieProperty<String>(
        id = id,
        name = name,
        unit = unit,
        retained = retained,
        datatype = "string",
        parentPublisher = parentPublisher,
        format = null) {
}


abstract class AbstractNumberProperty<T : Comparable<T>>(id: String,
                                                         name: String?,
                                                         parentPublisher: HomiePublisher,
                                                         retained: Boolean = true,
                                                         datatype: String,
                                                         unit: String? = null,
                                                         private val range: ClosedRange<T>?) : BaseHomieProperty<T>(
        id = id,
        name = name,
        parentPublisher = parentPublisher,
        retained = retained,
        unit = unit,
        datatype = datatype,
        format = range?.let { "${it.start}:${it.endInclusive}" }) {

    override fun update(t: T) {
        range?.containsOrThrow(t)
        super.update(t)
    }
}


class NumberProperty(id: String,
                     name: String?,
                     parentPublisher: HomiePublisher,
                     retained: Boolean = true,
                     unit: String? = null,
                     range: LongRange?) : AbstractNumberProperty<Long>(
        id = id,
        name = name,
        retained = retained,
        unit = unit,
        parentPublisher = parentPublisher,
        datatype = "integer",
        range = range)


class FloatProperty(id: String,
                    name: String?,
                    parentPublisher: HomiePublisher,
                    retained: Boolean = true,
                    unit: String? = null,
                    range: ClosedRange<Double>?) : AbstractNumberProperty<Double>(

        id = id,
        name = name,
        retained = retained,
        unit = unit,
        parentPublisher = parentPublisher,
        datatype = "float",
        range = range)



class EnumProperty<E : Enum<E>>(id: String,
                                name: String?,
                                parentPublisher: HomiePublisher,
                                retained: Boolean = true,
                                unit: String? = null,
                                enumValues: List<String>
) : BaseHomieProperty<E>(
        id = id,
        name = name,
        retained = retained,
        unit = unit,
        parentPublisher = parentPublisher,
        datatype = "enum",
        format = enumValues.joinToString(",")
)


class BoolProperty(id: String,
                   name: String?,
                   parentPublisher: HomiePublisher,
                   retained: Boolean = true,
                   unit: String? = null) : BaseHomieProperty<Boolean>(
        id = id,
        name = name,
        retained = retained,
        unit = unit,
        parentPublisher = parentPublisher,
        datatype = "boolean",
        format = null)

abstract class AbstractColorProperty<T>(id: String,
                                        name: String?,
                                        parentPublisher: HomiePublisher,
                                        retained: Boolean = true,
                                        colorType: String,
                                        unit: String? = null) : BaseHomieProperty<T>(
        id = id,
        name = name,
        retained = retained,
        unit = unit,
        parentPublisher = parentPublisher,
        datatype = "color",
        format = colorType)



class HSVColorProperty(id: String,
                       name: String?,
                       parentPublisher: HomiePublisher,
                       retained: Boolean = true,
                       unit: String? = null) : AbstractColorProperty<HSV>(
        id = id,
        name = name,
        retained = retained,
        unit = unit,
        parentPublisher = parentPublisher,
        colorType = "hsv") {

    override fun valueToString(hsv: HSV): String = "${hsv.hue},${hsv.saturation},${hsv.value}"

}

class RGBColorProperty(id: String,
                       name: String?,
                       parentPublisher: HomiePublisher,
                       retained: Boolean = true,
                       unit: String? = null) : AbstractColorProperty<RGB>(
        id = id,
        name = name,
        retained = retained,
        unit = unit,
        parentPublisher = parentPublisher,
        colorType = "rgb") {

    override fun valueToString(rgb: RGB): String = "${rgb.red},${rgb.green},${rgb.blue}"
}
