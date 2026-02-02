package de.msjones.android.alarmapp.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish

import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

class MqttClientWrapper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val serverUri: String,
    private val clientId: String,
    private val user: String,
    private val pass: String,
    private val topic: String,
    private val onMessage: (String) -> Unit,
    private val onState: (String) -> Unit,
    private val onAuthError: ((String) -> Unit)? = null
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

            val connAck: Mqtt3ConnAck = client?.connectWith()
                ?.simpleAuth()
                ?.username(user)
                ?.password(pass.toByteArray())
                ?.applySimpleAuth()
                ?.send()
                ?.await() ?: throw Exception("Verbindung fehlgeschlagen")

            // Check connection result
            if (connAck.returnCode.isError) {
                val errorMessage = "Verbindung abgelehnt: ${connAck.returnCode}"
                Log.e("MqttClientWrapper", "Connection error: $errorMessage")
                
                // Check if it's an auth error based on return code
                val returnCode = connAck.returnCode.toString().lowercase()
                val isAuthError = returnCode.contains("bad") || 
                                  returnCode.contains("auth") ||
                                  returnCode.contains("not authorized") ||
                                  returnCode.contains("identifier") ||
                                  returnCode.contains("credential")
                
                if (isAuthError) {
                    onAuthError?.invoke("Falscher Benutzername oder Passwort")
                } else {
                    onState("Verbindungsfehler: $errorMessage")
                }
                isConnected.set(false)
                return
            }

            isConnected.set(true)
            onState("Verbunden mit $serverUri")

            subscribe(topic)

        } catch (e: Exception) {
            val message = e.message ?: "Unbekannter Fehler"
            Log.e("MqttClientWrapper", "Connect error: $message", e)
            
            // Detect authentication-specific errors
            val msgLower = message.lowercase()
            val isAuthError = msgLower.contains("not authorized") ||
                              msgLower.contains("authentication failed") ||
                              msgLower.contains("bad username") ||
                              msgLower.contains("bad user") ||
                              msgLower.contains("connection refused") ||
                              msgLower.contains("identifier rejected") ||
                              msgLower.contains("credentials") ||
                              msgLower.contains("not_authorized") ||
                              msgLower.contains("not authorized") ||
                              msgLower.contains("auth")
            
            if (isAuthError) {
                Log.d("MqttClientWrapper", "Auth error detected, calling onAuthError callback")
                onAuthError?.invoke("Falscher Benutzername oder Passwort")
            } else {
                onState("Fehler beim Verbinden: $message")
            }
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

                    // Callback an MessagingService auf MainThread
                    lifecycleOwner.lifecycleScope.launch {
                        onMessage(message)
                    }
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
