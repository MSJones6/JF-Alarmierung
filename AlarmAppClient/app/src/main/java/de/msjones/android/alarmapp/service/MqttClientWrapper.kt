package de.msjones.android.alarmapp.service

import android.content.Context
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import kotlinx.coroutines.future.await
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

class MqttClientWrapper(
    private val context: Context,
    private val serverUri: String,
    private val clientId: String,
    private val user: String,
    private val pass: String,
    private val topic: String,
    private val onMessage: (String) -> Unit,
    private val onState: (String) -> Unit
) {

    private var client: Mqtt3AsyncClient? = null
    private val isConnected = AtomicBoolean(false)

    suspend fun connect() {
        try {
            client = MqttClient.builder()
                .useMqttVersion3()
                .identifier(clientId)
                .serverHost(serverUri.substringAfter("tcp://").substringBefore(":"))
                .serverPort(serverUri.substringAfterLast(":").toInt())
                .buildAsync()


//            fun connectAndListen() {
//                client?.connect()
//                    ?.whenComplete { connAck, throwable ->
//                        if (throwable != null) {
//                            println("❌ Verbindung fehlgeschlagen: $throwable")
//                        } else {
//                            println("✅ Verbunden mit Broker")
//
//                            // Nur abonnieren, kein Publish
//                            client?.subscribeWith()
//                                ?.topicFilter("JF/Alarm")
//                                ?.qos(MqttQos.AT_LEAST_ONCE)
//                                ?.callback { publish ->
//                                    val msg = publish.payloadAsBytes?.toString(Charsets.UTF_8) ?: "<leer>"
//                                    println("📩 Neue Nachricht empfangen: $msg")
//                                }
//                                ?.send()
//                                ?.whenComplete { subAck, subThrowable ->
//                                    if (subThrowable != null) {
//                                        println("❌ Subscribe fehlgeschlagen: $subThrowable")
//                                    } else {
//                                        println("✅ Erfolgreich subscribed auf test/topic")
//                                    }
//                                }
//                        }
//                    }
//            }

            client?.connectWith()
                ?.simpleAuth()
                ?.username(user)
                ?.password(pass.toByteArray())
                ?.applySimpleAuth()
                ?.send()

            isConnected.set(true)
            onState("Verbunden mit $serverUri")

            subscribe(topic)

        } catch (e: Exception) {
            Log.e("MqttClientWrapper", "Connect error: ${e.message}", e)
            onState("Fehler beim Verbinden: ${e.message}")
            isConnected.set(false)
        }
    }

    private suspend fun subscribe(topic: String) {
        try {
            client?.subscribeWith()
                ?.topicFilter(topic)
                ?.callback { publish: Mqtt3Publish ->
                    val payloadBytes = publish.payload
                        .map { buffer ->
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            bytes
                        }
                        .orElse(ByteArray(0))

                    val message = String(payloadBytes, StandardCharsets.UTF_8)
                    onMessage(message)
                }
                ?.send()
            onState("Abonniert: $topic")
        } catch (e: Exception) {
            Log.e("MqttClientWrapper", "Subscribe error: ${e.message}", e)
            onState("Fehler beim Abonnieren: ${e.message}")
        }
    }

    suspend fun disconnect() {
        try {
            client?.disconnect()?.await()
            isConnected.set(false)
            onState("Getrennt")
        } catch (e: Exception) {
            Log.e("MqttClientWrapper", "Disconnect error: ${e.message}", e)
            onState("Fehler beim Trennen: ${e.message}")
        }
    }

    fun isConnected(): Boolean = isConnected.get()
}
