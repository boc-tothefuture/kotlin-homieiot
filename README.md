# kotlin-homieiot

This is a work in progress kotlin client implementation of the [homie protocol.](https://git.io/homieiot)

# Example
The goal is to provide a kotlin-dsl to support the homieiot protocol.

```
device(id = "hello-world") {
        node(id = "echo", type = "basic") {
            string(id = "text", value = "Hello, World!")
        }
    }
```

The above created a device with id of "hello-world", establishes a node
with id "echo" and a property with id of "text", whose value is "Hello, World!".


