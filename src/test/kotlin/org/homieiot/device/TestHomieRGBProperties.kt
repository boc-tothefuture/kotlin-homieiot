package org.homieiot.device

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestHomieRGBProperties {

    @Test
    fun `Test Property Config`() {

        val publisherMock = PublisherFake()

        val property = RGBColorProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher)

        property.publishConfig()

        Assertions.assertThat(publisherMock.messagePairs).containsAll(listOf(
                Triple("foo" / "datatype".attr(), "color", true),
                Triple("foo" / "format".attr(), "rgb", true)
        ))
    }


    @Test
    fun `Test Data Type Projection`() {

        val publisherMock = PublisherFake()
        var messageReceived: RGB? = null

        val property = RGBColorProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher)
        property.subscribe { messageReceived = it.update }


        val message = RGB(50, 50, 50)
        property.mqttReceived("50,50,50")
        Assertions.assertThat(messageReceived).isNotNull.isEqualTo(message)
        publisherMock.messagePairs.clear()

        property.update(RGB(100, 100, 100))
        Assertions.assertThat(publisherMock.messagePairs).hasSize(1).last().isEqualTo(Triple("foo", "100,100,100", true))
    }


    @Test
    fun `Test DSL Function`() {

        val nodeFake = NodeFake()
        val node = nodeFake.node()
        var rgbProperty: HomieProperty<RGB>? = null
        node.rgb(id = "rgb", retained = false, name = "foo", unit = "bar") {
            rgbProperty = this

        }

        Assertions.assertThat(rgbProperty).isNotNull

        rgbProperty!!.publishConfig()

        Assertions.assertThat(nodeFake.publishedMessages).containsExactlyInAnyOrder(
                messageFor("homie", "device", "node", "\$properties", payload = "rgb"),
                messageFor("homie", "device", "node", "rgb", "\$name", payload = "foo"),
                messageFor("homie", "device", "node", "rgb", "\$retained", payload = "false"),
                messageFor("homie", "device", "node", "rgb", "\$settable", payload = "false"),
                messageFor("homie", "device", "node", "rgb", "\$datatype", payload = "color"),
                messageFor("homie", "device", "node", "rgb", "\$format", payload = "rgb"),
                messageFor("homie", "device", "node", "rgb", "\$unit", payload = "bar")
        )


    }

}