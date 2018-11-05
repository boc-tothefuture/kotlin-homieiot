package org.homieiot.device

import org.homieiot.mqtt.RootHomiePublisher
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


@DslMarker
annotation class DeviceTagMarker


interface HomieUnit {
    fun publishConfig()
}


fun String.homieAttribute(): String {
    return "\$${this}"
}


/**
 * Simple Observable that invokes lambda with no parameters when a value changes.
 * @param initialValue the initial value of the property.
 * @param onChange the callback which is called after the change of the property is made. The value of the property
 *  has already been changed when this callback is invoked.
 *
 */
internal inline fun <T> simpleObservable(initialValue: T, crossinline onChange: () -> Unit):
        ReadWriteProperty<Any?, T> =
        object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = onChange()
        }


public enum class HomieState { INIT, READY, DISCONNECTED, SLEEPING, LOST, ALERT }

@DeviceTagMarker
class HomieDevice(private val id: String, private val name: String = id, private val baseTopic: String = "homie") : HomieUnit {

    private val IMPLEMENTATION = "kotlin-homie"

    private val HOMIE_VERSION = "3.1.0"

    private val nodes = mutableListOf<HomieNode>()

    var state: HomieState by simpleObservable(HomieState.INIT) { publishState() }

    internal val publisher: RootHomiePublisher = RootHomiePublisher(topicParts = listOf(baseTopic, id))

    fun node(id: String, type: String, name: String = id, init: HomieNode.() -> Unit): HomieNode {
        val node = HomieNode(id = id, type = type, name = name, parentPublisher = publisher)
        node.init()
        nodes.add(node)
        publishNodes()
        return node
    }

    override fun publishConfig() {
        publisher.publishMessage(topicSegment = "homie".homieAttribute(), payload = HOMIE_VERSION)
        publisher.publishMessage(topicSegment = "name".homieAttribute(), payload = name)
        publisher.publishMessage(topicSegment = "implementation".homieAttribute(), payload = IMPLEMENTATION)
        publishState()
        publishNodes()
    }

    private fun publishState() {
        publisher.publishMessage(topicSegment = "\$state", payload = state.toString().toLowerCase())
    }

    private fun publishNodes() {
        publisher.publishMessage(topicSegment = "\$nodes", payload = nodes.joinToString(",") { it.id })
    }


}


fun device(id: String, name: String = id, init: HomieDevice.() -> Unit): HomieDevice {
    val device = HomieDevice(id, name)
    device.init()
    return device
}