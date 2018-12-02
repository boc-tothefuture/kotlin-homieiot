package org.homieiot

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestID {


    @Test
    fun `Cannot be empty`() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { idRequire("") }
    }

    @Test
    fun `Cannot start with underscore`() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { idRequire("_foo") }
    }

    @Test
    fun `Cannot end with underscore`() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { idRequire("foo_") }
    }

    @Test
    fun `Cannot end with hyphen`() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { idRequire("foo-") }
    }

    @Test
    fun `Cannot start with hyphen`() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { idRequire("-foo") }
    }

    @Test
    fun `Cannot contain  dollar`() {
        Assertions.assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy { idRequire("foo\$foo") }
    }

    @Test
    fun `Can contain lowercase letters and numbers`() {
        idRequire("foo")
        idRequire("5foo")
        idRequire("foo5")
    }

    @Test
    fun `Can contain lowercase letters and numbers hyphens and underscores`() {
        idRequire("foo_foo")
        idRequire("foo-foo")
    }

}
