package org.homieiot.device

abstract class HomieProperty<T>(val id: String, val name: String?, val settable: Boolean = false, val unit: String?, val datatype: String, val format: String?, var value: T?) {
    fun valueToString(): String? {
        return value?.toString()
    }
}

class StringProperty(id: String, name: String?, settable: Boolean = false, unit: String?, value: String?) : HomieProperty<String>(
        id = id,
        name = name,
        settable = settable,
        unit = unit,
        datatype = "string",
        format = null,
        value = value
)

class IntProperty(id: String, name: String?, settable: Boolean = false, unit: String?, private val range: IntRange?, value: Int?) : HomieProperty<Int>(
        id = id,
        name = name,
        settable = settable,
        unit = unit,
        datatype = "integer",
        format = range?.let { "${it.first}:${it.last}" },
        value = value
)