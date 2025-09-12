package de.msjones.android.alarmapp.service

import com.rabbitmq.client.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import android.util.Log

data class AmqpConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val queue: String
)

class AmqpClient(
    private val cfg: AmqpConfig,
    private val onMessage: (String) -> Unit,
    private val onState: (String) -> Unit
) {
    @Volatile private var connection: Connection? = null
    @Volatile private var channel: Channel? = null

    fun isConnected(): Boolean = connection?.isOpen == true && channel?.isOpen == true

    fun connect() {
        val factory = ConnectionFactory().apply {
            host = cfg.host
            port = cfg.port
            username = cfg.username
            password = cfg.password
            isAutomaticRecoveryEnabled = true
            networkRecoveryInterval = 5000
            requestedHeartbeat = 30
        }
        onState("Verbinde zu ${cfg.host}:${cfg.port} …")
        val conn = factory.newConnection("AndroidClient")
        val ch = conn.createChannel()
        ch.basicQos(1)
        ch.queueDeclare(cfg.queue, true, false, false, null)
        connection = conn
        channel = ch
        onState("Verbunden")
        startConsumer()
    }

    private fun startConsumer() {
        val ch = channel ?: return
        val consumer = object : DefaultConsumer(ch) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope?,
                properties: AMQP.BasicProperties?,
                body: ByteArray?
            ) {
                val msg = body?.toString(Charsets.UTF_8) ?: ""
                onMessage(msg)
                envelope?.let { ch.basicAck(it.deliveryTag, false) }
            }
        }
        ch.basicConsume(cfg.queue, false, consumer)
    }

    fun close() {
        try { channel?.close() } catch (_: Exception) {}
        try { connection?.close() } catch (_: Exception) {}
        channel = null
        connection = null
    }

    suspend fun keepAlive(stopFlag: suspend () -> Boolean) {
        var attempt = 0
        while (!stopFlag()) {
            try {
                if (!isConnected()) {
                    connect()
                    attempt = 0
                }
                repeat(30) {
                    if (stopFlag()) return
                    delay(2.seconds)
                    yield()
                }
            } catch (t: Throwable) {
                Log.e("Verbindung: ", t.message,t)
                onState("Verbindungsfehler: ${t.message}")
                close()
                attempt++
                val backoff = when {
                    attempt < 5 -> 2.seconds
                    attempt < 10 -> 10.seconds
                    else -> 1.minutes
                }
                delay(backoff)
            }
        }
        close()
    }
}
