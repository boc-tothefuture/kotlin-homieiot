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


internal fun String.homieAttribute(): String {
    return "\$$this"
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


enum class HomieState { INIT, READY, DISCONNECTED, SLEEPING, LOST, ALERT }

@DeviceTagMarker
class HomieDevice(private val id: String, private val name: String = id, baseTopic: String = "homie") : HomieUnit {

    companion object {
        private const val IMPLEMENTATION = "kotlin-homie"
        private const val HOMIE_VERSION = "3.1.0"
        private const val STATE_SUB_TOPIC = "\$state"
    }

    private val nodes = mutableMapOf<String, HomieNode>()

    var state: HomieState by simpleObservable(HomieState.INIT) { publishState() }

    internal val publisher: RootHomiePublisher = RootHomiePublisher(topicParts = listOf(baseTopic, id))

    internal val stateTopic = listOf(baseTopic, id, STATE_SUB_TOPIC).joinToString("/")


    /**
     * Publish the devices and optionally the nodes/properties configuration.
     * @param recursive If true, the nodes and properties of those nodes have their configuration also published
     */
    internal fun publishConfig(recursive: Boolean) {
        publisher.publishMessage(topicSegment = "homie".homieAttribute(), payload = HOMIE_VERSION)
        publisher.publishMessage(topicSegment = "name".homieAttribute(), payload = name)
        publisher.publishMessage(topicSegment = "implementation".homieAttribute(), payload = IMPLEMENTATION)
        publishState()
        publishNodes()
        if (recursive) nodes.values.forEach { it.publishConfig(true) }
    }

    /**
     * Publish the devices configuration
     */
    override fun publishConfig() = publishConfig(false)

    internal val settablePropertyMap: Map<List<String>, HomieProperty<*>>
        get() = nodes.values.flatMap { it.properties.values }.filter { it.settable }.associate { it.topicSegments + "set" to it }


    private fun publishState() {
        publisher.publishMessage(topicSegment = STATE_SUB_TOPIC, payload = state.toString().toLowerCase())
    }

    private fun publishNodes() {
        publisher.publishMessage(topicSegment = "nodes".homieAttribute(), payload = nodes.values.joinToString(",") { it.id })
    }

    private fun addNode(node: HomieNode) {
        if (nodes.containsKey(node.id)) {
            throw IllegalArgumentException("Duplicate node IDs are not allowed - duplicate id ($node.id)")
        }
        nodes[node.id] = node
    }


    fun node(id: String, type: String, name: String = id, init: HomieNode.() -> Unit): HomieNode {
        val node = HomieNode(id = id, type = type, name = name, parentPublisher = publisher)
        node.init()
        addNode(node)
        publishNodes()
        return node
    }

    fun node(id: String, type: String, name: String = id, range: IntRange, init: HomieNode.(index: Int) -> Unit) {
        range.forEach { index ->
            val node = HomieNode(id = "id-$index", type = type, name = name, parentPublisher = publisher)
            node.init(index)
            addNode(node)
        }
        publishNodes()
    }

}


fun device(id: String, name: String = id, init: HomieDevice.() -> Unit): HomieDevice {
    val device = HomieDevice(id, name)
    device.init()
    return device
}

