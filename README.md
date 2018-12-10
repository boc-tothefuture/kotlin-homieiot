# kotlin-homieiot [![Build Status](https://travis-ci.com/boc-tothefuture/kotlin-homieiot.svg?branch=master)](https://travis-ci.com/boc-tothefuture/kotlin-homieiot) [![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](https://github.com/boc-tothefuture/kotlin-homieiot/edit/master/LICENSE)

An opinionated Kotlin client implementation of the [homie IoT protocol](https://git.io/homieiot)
utilizing [eclipse paho](https://www.eclipse.org/paho/) for MQTT communication.

# Simple Example
Create a Homie IoT property located at `/homie/hello-world/echo/text` with a value of "Hello, World!" and connect
to an MQTT server based on environment variables.

```
device(id = "hello-world") {
        node(id = "echo", type = "basic") {
            string(id = "text", value = "Hello, World!")
        }
    }
HomieMqttClient.fromEnv(device).connect()    
```


# Environment variables
The HomieMqttClient can be [configured manually](https://boc-tothefuture.github.io/kotlin-homieiot/kotlin-homie/org.homieiot.mqtt/-homie-mqtt-client/index.html)
or the `HomieMqttClient.fromEnv` method may be used to extract the appropriate values from the environment.
Processed environment variables include:

| Variable      | Description           | Required  |
| ------------- |-----------------------| -----     | 
| MQTT_SERVER   | Format is tcp://host:port  | Yes  |
| MQTT_CLIENT_ID  | [Client ID](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_7.1.0/com.ibm.mq.doc/tt60310_.htm)  | Yes  |
| MQTT_USERNAME | Username for MQTT      |   NO |
| MQTT_PASSWORD | Password for MQTT |    NO |



# Complete Example
The following code is a complete example of using this library to publish and receive information from a Nuvo
music player using the the Homie IoT convention.  The Nuvo client library provides updates using an [rxBus implementation](https://android.jlelse.eu/rxbus-kotlin-listen-where-ever-you-want-e6fc0760a4a8)
and is external to this example.

```
import org.homieiot.PropertyType
import org.homieiot.device
import org.homieiot.mqtt.HomieMqttClient
import org.nuvo.Client
import org.nuvo.event.*
import org.nuvo.message.transmit.SetSourceDisplayLine
import org.nuvo.message.transmit.SetSourceTrackStatus
import kotlin.properties.Delegates

enum class Command {
    NEXT, PLAYPAUSE, PREV
}


private val client = Client()

fun main() {

    val device = device(id = "nuvo") {
        node(id = "source", name = "NuVo Source", type = "Audio Source", range = 1..4) { source ->
            for (lineNum in 1..4) {
                string(id = "displayLine-$lineNum", name = "Display Line $lineNum") {
                    eventBusUpdate<SourceDisplay> { if (it.source == source && it.lineNumber == lineNum) update(it.line) }
                    subscribe {
                        client.sendMessage(
                            SetSourceDisplayLine(
                                source = source,
                                lineNumber = lineNum,
                                line = it.update
                            )
                        )
                    }
                }
            }
            val trackStatus = TrackStatusPublisher(client, source)

            number(id = "duration", name = "Track Duration") {
                eventBusUpdate<TrackStatus> { if (it.source == source) update(it.duration.toLong()) }
                subscribe { trackStatus.duration = it.update }
            }
            number(id = "position", name = "Track Position") {
                eventBusUpdate<TrackStatus> { if (it.source == source) update(it.position.toLong()) }
                subscribe { trackStatus.position = it.update }
            }
            enum<Status>(id = "status", name = "Source Status") {
                eventBusUpdate<TrackStatus> { if (it.source == source) update(it.status) }
                subscribe { trackStatus.status = it.update }
            }
        }
        node(id = "zone", name = "NuVo Zone", type = "Audio Zone", range = 1..6) { zone ->
            number(id = "source", name = "Zone $zone Source") {
                eventBusUpdate<ZoneSource> { if (it.zone == zone) update(it.source.toLong()) }
            }
            number(id = "volume", name = "Zone $zone Volume") {
                eventBusUpdate<ZoneVolume> { if (it.zone == zone) update(it.level.toLong()) }
            }
            bool(id = "power", name = "Zone $zone Power") {
                eventBusUpdate<ZonePower> { if (it.zone == zone) update(it.powerState == PowerState.ON) }
            }
            bool(id = "mute", name = "Zone $zone Mute") {
                eventBusUpdate<ZoneMute> { if (it.zone == zone) update(it.muteState == MuteState.ON) }
            }

            enum<Command>(id = "command", name = "Zone $zone Transport Command", type = PropertyType.EVENT) {
                eventBusUpdate<Next> { if (it.zone == zone) update(Command.NEXT) }
                eventBusUpdate<Prev> { if (it.zone == zone) update(Command.PREV) }
                eventBusUpdate<PlayPause> { if (it.zone == zone) update(Command.PLAYPAUSE) }
                subscribe {
                    when (it.update) {
                        Command.PLAYPAUSE -> client.sendMessage(org.nuvo.message.transmit.PlayPause(zone))
                        Command.NEXT -> client.sendMessage(org.nuvo.message.transmit.Next(zone))
                        Command.PREV -> client.sendMessage(org.nuvo.message.transmit.Prev(zone))
                    }
                }
            }
        }
    }

    HomieMqttClient.fromEnv(device).connect()
    client.connect()
}

/** * This function creates an object that subscribes to updates from the event bus
 */
private inline fun <reified T> eventBusUpdate(noinline update: (t: T) -> Unit) {
    client.eventBus.listen(T::class.java).subscribe {
        update.invoke(it)
    }
}


class TrackStatusPublisher(private val client: Client, private val source: Int) {

    var duration: Long? by Delegates.observable<Long?>(null) { _, _, _ -> statusUpdate(duration,position,status) }
    var position: Long? by Delegates.observable<Long?>(null) { _, _, _ -> statusUpdate(duration,position,status) }
    var status: Status? by Delegates.observable<Status?>(null) { _, _, _ -> statusUpdate(duration,position,status) }

    private fun statusUpdate(duration: Long?, position: Long?, status: Status?) {
        if (duration != null && position != null && status != null) {
            client.sendMessage(
                SetSourceTrackStatus(
                    source,
                    duration = duration.toInt(),
                    position = position.toInt(),
                    status = status
                )
            )
        }
    }
}


```

# Documentation
Complete [API Documentation](https://boc-tothefuture.github.io/kotlin-homieiot/kotlin-homie/)
