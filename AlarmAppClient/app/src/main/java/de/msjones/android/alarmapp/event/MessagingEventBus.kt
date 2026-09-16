package de.msjones.android.alarmapp.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Ereignisse aus dem Messaging-Service für die UI-Schicht.
 */
sealed class MessagingEvent {
    /**
     * Eine neue Alarmnachricht wurde empfangen.
     */
    data class NewMessage(
        val keyword: String,
        val location: String,
        val extras: String
    ) : MessagingEvent()

    /**
     * Der Verbindungsstatus einer MQTT-Verbindung hat sich geändert.
     */
    data class ConnectionState(
        val status: String,
        val message: String
    ) : MessagingEvent()

    /**
     * Authentifizierung am Broker ist fehlgeschlagen.
     */
    data class AuthError(
        val errorMessage: String
    ) : MessagingEvent()

    /**
     * Alle Verbindungen sollen beendet werden (z. B. nach Fehler).
     */
    data object StopAllConnections : MessagingEvent()

    /**
     * Der Foreground-Service läuft oder wurde gestoppt.
     */
    data class ServiceRunningState(
        val isRunning: Boolean
    ) : MessagingEvent()
}

/**
 * Prozessweiter Event-Bus auf Basis von [SharedFlow] als Ersatz für LocalBroadcastManager.
 */
object MessagingEventBus {

    private val _events = MutableSharedFlow<MessagingEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /** Lesbarer Strom aller Messaging-Ereignisse. */
    val events: SharedFlow<MessagingEvent> = _events.asSharedFlow()

    /**
     * Sendet ein Ereignis an alle aktiven Collector.
     *
     * @param event das zu veröffentlichende Ereignis
     */
    fun tryEmit(event: MessagingEvent): Boolean = _events.tryEmit(event)
}
