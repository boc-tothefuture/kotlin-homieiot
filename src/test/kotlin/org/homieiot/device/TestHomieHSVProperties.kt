package org.homieiot.device

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestHomieHSVProperties {

    @Test
    fun `Test Property Config`() {

        val publisherMock = PublisherFake()

        val property = HSVColorProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher)

        property.publishConfig()

        Assertions.assertThat(publisherMock.messagePairs).containsAll(listOf(
                Triple("foo" / "datatype".attr(), "color", true),
                Triple("foo" / "format".attr(), "hsv", true)
        ))
    }


    @Test
    fun `Test Data Type Projection`() {

        val publisherMock = PublisherFake()
        var messageReceived: HSV? = null

        val property = HSVColorProperty(id = "foo", name = "bar", parentPublisher = publisherMock.publisher)
        property.subscribe { messageReceived = it.update }


        val message = HSV(50, 50, 50)
        property.mqttReceived("50,50,50")
        Assertions.assertThat(messageReceived).isNotNull.isEqualTo(message)
        publisherMock.messagePairs.clear()

        property.update(HSV(100, 100, 100))
        Assertions.assertThat(publisherMock.messagePairs).hasSize(1).last().isEqualTo(Triple("foo", "100,100,100", true))
    }


    @Test
    fun `Test DSL Function`() {

        val nodeFake = NodeFake()
        val node = nodeFake.node()
        var hsvProperty: HomieProperty<HSV>? = null
        node.hsv(id = "hsv", retained = false, name = "foo", unit = "bar") {
            hsvProperty = this

        }

        Assertions.assertThat(hsvProperty).isNotNull

        hsvProperty!!.publishConfig()

        Assertions.assertThat(nodeFake.publishedMessages).containsExactlyInAnyOrder(
                messageFor("homie", "device", "node", "\$properties", payload = "hsv"),
                messageFor("homie", "device", "node", "hsv", "\$name", payload = "foo"),
                messageFor("homie", "device", "node", "hsv", "\$retained", payload = "false"),
                messageFor("homie", "device", "node", "hsv", "\$settable", payload = "false"),
                messageFor("homie", "device", "node", "hsv", "\$datatype", payload = "color"),
                messageFor("homie", "device", "node", "hsv", "\$format", payload = "hsv"),
                messageFor("homie", "device", "node", "hsv", "\$unit", payload = "bar")
        )


    }

}