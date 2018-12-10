# kotlin-homieiot [![Build Status](https://travis-ci.com/boc-tothefuture/kotlin-homieiot.svg?branch=master)](https://travis-ci.com/boc-tothefuture/kotlin-homieiot) [![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](https://github.com/boc-tothefuture/kotlin-homieiot/edit/master/LICENSE)

An opinionated kotlin client implementation of the [homie IoT protocol.](https://git.io/homieiot)
A convenient library utilizing [eclipse paho](https://www.eclipse.org/paho/) for MQTT communication.

# Example
This library provides a kotlin-dsl to support the homieiot protocol.

```
device(id = "hello-world") {
        node(id = "echo", type = "basic") {
            string(id = "text", value = "Hello, World!")
        }
    }
```

The above created a device with id of "hello-world", establishes a node
with id "echo" and a property with id of "text", whose value is "Hello, World!".

# Documentation
[API Documentation](https://boc-tothefuture.github.io/kotlin-homieiot/kotlin-homie/)
