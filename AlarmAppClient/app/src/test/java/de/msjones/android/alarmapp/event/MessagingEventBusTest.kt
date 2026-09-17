package de.msjones.android.alarmapp.event

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-Tests für den [MessagingEventBus].
 */
class MessagingEventBusTest {

    /** Prüft, dass Events an Collector zugestellt werden. */
    @Test
    fun tryEmit_deliversEventToCollector() = runBlocking {
        val deferred = async {
            withTimeout(2000) {
                MessagingEventBus.events.first {
                    it is MessagingEvent.ServiceRunningState && it.isRunning
                }
            }
        }
        // Kurz warten, bis der Collector aktiv ist
        kotlinx.coroutines.delay(50)
        assertTrue(MessagingEventBus.tryEmit(MessagingEvent.ServiceRunningState(true)))
        val event = deferred.await() as MessagingEvent.ServiceRunningState
        assertTrue(event.isRunning)
    }

    /** Prüft, dass ConnectionState die Verbindungs-ID trägt. */
    @Test
    fun tryEmit_connectionStateCarriesConnectionId() = runBlocking {
        val deferred = async {
            withTimeout(2000) {
                MessagingEventBus.events.first {
                    it is MessagingEvent.ConnectionState && it.connectionId == "conn-1"
                }
            }
        }
        kotlinx.coroutines.delay(50)
        assertTrue(
            MessagingEventBus.tryEmit(
                MessagingEvent.ConnectionState("CONNECTED", "Verbunden", "conn-1")
            )
        )
        val event = deferred.await() as MessagingEvent.ConnectionState
        assertEquals("CONNECTED", event.status)
        assertEquals("Verbunden", event.message)
        assertEquals("conn-1", event.connectionId)
    }

    /** Prüft NewMessage-Payload. */
    @Test
    fun tryEmit_newMessageCarriesPayload() = runBlocking {
        val deferred = async {
            withTimeout(2000) {
                MessagingEventBus.events.first { it is MessagingEvent.NewMessage }
            }
        }
        kotlinx.coroutines.delay(50)
        MessagingEventBus.tryEmit(
            MessagingEvent.NewMessage("Brand", "Ort", "Info")
        )
        val event = deferred.await() as MessagingEvent.NewMessage
        assertEquals("Brand", event.keyword)
        assertEquals("Ort", event.location)
        assertEquals("Info", event.extras)
    }
}
