package org.homieiot

import org.homieiot.mqtt.RootHomiePublisher
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/**
 * @suppress
 */
@DslMarker
annotation class DeviceTagMarker


/**
 * Converts the string to a homie attribute by prepending it with $
 */
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


/**
 *
 * A device represents a physical piece of hardware. For example, an Arduino/ESP8266 or a coffee machine.
 *
 * A device is composed of one or more [org.homieiot.Node] objects.  Nodes are independent or logically separable parts of a device.
 *
 * Nodes are themselves composed by [org.homieiot.Property] objects.  Properties represent basic characteristics of the node/device, often given as numbers or finite states.
 *
 */
@DeviceTagMarker
class Device internal constructor(private val id: String, private val name: String = id) {


    /**
     * The current state of the device.
     */
    enum class State {

        /**
         * This is the state the device is in when it is ready to operate.
         */
        READY,
        /**
         * This is the state the device is in when it is disconnected.
         */
        DISCONNECTED,
        /**
         * This is the state the device is in when the device is sleeping.
         */
        SLEEPING,
        /**
         * This is the state the device is when connected, but something wrong is happening. E.g. a sensor is not providing data and needs human intervention.
         */
        ALERT
    }
    

    /**
     * Representation of the state that the homie convention supports for devices.
     * This is a super-set of the state available to consumers of the API.
     */
    internal enum class InternalState {
        INIT,
        READY,
        DISCONNECTED,
        SLEEPING,
        LOST,
        ALERT;

        override fun toString(): String {
            return super.toString().toLowerCase()
        }
    }

    companion object {
        private const val IMPLEMENTATION = "kotlin-homie"
        private const val HOMIE_VERSION = "3.0.1"
        private const val STATE_SUB_TOPIC = "\$state"
    }

    init {
        idRequire(id)
    }

    private val nodes = mutableMapOf<String, Node>()

    internal var state: InternalState by simpleObservable(InternalState.INIT) { publishState() }

    internal val publisher: RootHomiePublisher = RootHomiePublisher(topicParts = listOf(id))

    internal val stateTopic = listOf(id, STATE_SUB_TOPIC).joinToString("/")


    /**
     * Publish the devices and optionally the nodes/properties configuration.
     * @param includeNodes If true, the nodes and properties of those nodes have their configuration also published
     */
    internal fun publishConfig(includeNodes: Boolean) {
        publisher.publishMessage(topicSegment = "homie".homieAttribute(), payload = HOMIE_VERSION)
        publisher.publishMessage(topicSegment = "name".homieAttribute(), payload = name)
        publisher.publishMessage(topicSegment = "implementation".homieAttribute(), payload = IMPLEMENTATION)
        publishState()
        publishNodes()
        if (includeNodes) nodes.values.forEach { it.publishConfig(true) }
    }

    /**
     * Publish the devices configuration
     */
    internal fun publishConfig() = publishConfig(false)

    internal val settablePropertyMap: Map<List<String>, BaseProperty<*>>
        get() = nodes.values.flatMap { it.properties.values }.filter { it.settable }.associate { it.topicSegments + "set" to it }


    /**
     * Publish the current state of the device
     */
    private fun publishState() {
        publisher.publishMessage(topicSegment = STATE_SUB_TOPIC, payload = state.toString().toLowerCase())
    }

    /**
     * Publish the list of nodes for this device
     */
    private fun publishNodes() {
        publisher.publishMessage(topicSegment = "nodes".homieAttribute(), payload = nodes.values.joinToString(",") { it.id })
    }

    private fun addNode(node: Node) {
        if (nodes.containsKey(node.id)) throw IllegalArgumentException("Duplicate node IDs are not allowed - duplicate id ($node.id)")
        nodes[node.id] = node
    }


    /**
     * Set the [state] based on the state of the physical or virtual device.
     */
    fun state(state: State) {
        when (state) {
            State.READY -> this.state = InternalState.READY
            State.ALERT -> this.state = InternalState.ALERT
            State.DISCONNECTED -> this.state = InternalState.DISCONNECTED
            State.SLEEPING -> this.state = InternalState.SLEEPING
        }

    }


    /**
     * Add a [org.homieiot.Node] to this device
     *
     * @param id Each node must have a unique device ID which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids)
     * @param name Friendly name of the node
     * @param type Description of the node type, for example if the node was a car engine, the type may be "V8"
     * @param [init] Function that supplies this node to enable adding properties to the node
     */
    fun node(id: String, type: String, name: String = id, init: Node.() -> Unit): Node {
        val node = Node(id = id, type = type, name = name, parentPublisher = publisher)
        node.init()
        addNode(node)
        publishNodes()
        return node
    }

    /**
     * Add a range of [org.homieiot.Node] to this device
     *
     * @param id Each node must have a unique device ID which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids).
     * @param name Friendly name of the node
     * @param type Description of the node type, for example if the node was a car engine, the type may be "V8"
     * @param range Creates multiple nodes, automatically incrementing the id of each with "-" and a number in the range. For example, if the [id] is "foo" and [range] is 1..3 nodes with ids "foo-1", "foo-2" and "foo-3" will be created.
     * @param [init] Function that takes the index of an node and this node itself to enable adding properties to the node
     */
    fun node(id: String, type: String, name: String = id, range: IntRange, init: Node.(index: Int) -> Unit) {
        range.forEach { index ->
            val node = Node(id = "$id-$index", type = type, name = name, parentPublisher = publisher)
            node.init(index)
            addNode(node)
        }
        publishNodes()
    }

}


/**
 * Create a new device instance
 *
 * Commonly, a device represents a physical piece of hardware. For example, an Arduino/ESP8266 or a coffee machine.
 * @param id Each device must have a unique device ID which adheres to the [homie id convention](https://homieiot.github.io/specification/spec-core-v3_0_1/#topic-ids)
 * @param name Friendly name of the device
 * @param init Add the nodes within the init block
 */
fun device(id: String, name: String = id, init: Device.() -> Unit): Device {
    val device = Device(id, name)
    device.init()
    return device
}

