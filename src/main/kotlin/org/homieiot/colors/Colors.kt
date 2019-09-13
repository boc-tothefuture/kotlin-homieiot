package org.homieiot.colors

import org.homieiot.containsOrThrow

/**
 * A color model represented as three values: [hue], [saturation], and [value]
 *
 * @constructor Create a color model based on [hue], [saturation] and [value]
 * @property hue The attribute of a visual sensation according to which an area appears to be similar to one of the perceived colors: red, yellow, green, and blue, or to a combination of two of them. Valid values are between 0 and 360.
 * @property saturation The colorfulness of a stimulus relative to its own brightness. Valid values are between 0 and 100.
 * @property value The brightness relative to the brightness of a similarly illuminated white. Valid values are between0 and 100.
 */
data class HSV(val hue: Int, val saturation: Int, val value: Int) {

    /**
     * @constructor Create a color model using a triple, the first value is [hue], the second is [saturation] and the third is [value]
     */
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

/**
 * An additive color model in which [red], [green] and [blue] light are added together in various ways to reproduce a broad array of colors.
 *
 * @constructor Create a color model based on [red], [green] and [blue] values
 * @property red The amount of red in the color model. Valid values are from 0 to 255.
 * @property green The amount of green in the color model. Valid values are from 0 to 255.
 * @property blue The amount of blue in the color model. Valid values are from 0 to 255.
 */
data class RGB(val red: Int, val green: Int, val blue: Int) {

    /**
     * @constructor Create a color model using a triple, the first value is [red], the second is [green] and the third is [blue]
     */
    constructor(triple: Triple<Int, Int, Int>) : this(triple.first, triple.second, triple.third)

    init {
        listOf(red, green, blue).forEach { RGB_RANGE.containsOrThrow(it) }
    }

    private companion object {
        val RGB_RANGE = 0..255
    }
}