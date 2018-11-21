package org.homieiot.device


data class HSV(val hue: Int, val saturation: Int, val value: Int) {


    constructor(triple: Triple<Int, Int, Int>) : this(triple.first, triple.second, triple.third)


    init {
        HUE_RANGE.containsOrThrow(hue)
        SATURATION_RANGE.containsOrThrow(saturation)
        VALUE_RANGE.containsOrThrow(value)
    }

    private companion object {
        val HUE_RANGE = 0..360
        val SATURATION_RANGE = 0..100
        val VALUE_RANGE = 0..100
    }
}

data class RGB(val red: Int, val green: Int, val blue: Int) {

    constructor(triple: Triple<Int, Int, Int>) : this(triple.first, triple.second, triple.third)

    init {
        listOf(red, green, blue).forEach { RGB_RANGE.containsOrThrow(it) }
    }

    private companion object {
        val RGB_RANGE = 0..255
    }
}