package org.homieiot.device


fun main(args: Array<String>) {
    val homieDevice =
            device(id = "foo", name = "foo") {
                node(id = "bar", type = "test") {

                }
                node(id = "baz", type = "test") {
                    string(id = "hello", value = "world")
                    int(id = "hello2", value = 1, range = 1..20)
                }
            }

    println(homieDevice.toHomieMessages())
}