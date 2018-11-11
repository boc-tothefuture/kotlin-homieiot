package org.homieiot.device

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TestColors {

    @ParameterizedTest
    @CsvSource("0,0,0", "360,100,100", "50,50,50")
    fun `Test in HSV Range`(hue: Int, saturation: Int, value: Int) {
        HSV(hue = hue, saturation = saturation, value = value)
    }

    @ParameterizedTest
    @CsvSource("-1,0,0", "361,50,50", "50,-1,50", "50,101,50", "50,50,-1", "50,50,101")
    fun `Test out of HSV Range`(hue: Int, saturation: Int, value: Int) {

        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            HSV(hue = hue, saturation = saturation, value = value)
        }
    }


    @ParameterizedTest
    @CsvSource("0,0,0", "255,255,255", "50,50,50")
    fun `Test in RGB Range`(red: Int, green: Int, blue: Int) {
        RGB(red = red, green = green, blue = blue)
    }

    @ParameterizedTest
    @CsvSource("-1,0,0", "256,50,50", "50,-1,50", "50,256,50", "50,50,-1", "50,50,256")
    fun `Test out of RGB Range`(red: Int, green: Int, blue: Int) {

        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            RGB(red = red, green = green, blue = blue)
        }
    }


}