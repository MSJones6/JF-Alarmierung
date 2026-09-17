package de.msjones.android.alarmapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit-Tests für [ConnectionStatusTexts] und [ConnectionPhase].
 */
class ConnectionStatusTextsTest {

    /** Alle aktiven Verbindungen ergeben das bisherige Verhältnis. */
    @Test
    fun summary_formatsActiveOfTotal() {
        val phases = listOf(ConnectionPhase.ACTIVE, ConnectionPhase.OFFLINE)
        assertEquals("1 von 2 Verbindungen aktiv", ConnectionStatusTexts.summary(phases))
        assertEquals(
            "2 von 2 Verbindungen aktiv",
            ConnectionStatusTexts.summary(listOf(ConnectionPhase.ACTIVE, ConnectionPhase.ACTIVE))
        )
    }

    /** Ohne laufende Verbindung erscheint Offline. */
    @Test
    fun summary_withoutLiveConnections() {
        assertEquals("Offline", ConnectionStatusTexts.summary(emptyList()))
        assertEquals(
            "Offline",
            ConnectionStatusTexts.summary(listOf(ConnectionPhase.OFFLINE, ConnectionPhase.OFFLINE))
        )
    }

    /** Der erste Verbindungsaufbau wird als Verbinden dargestellt. */
    @Test
    fun summary_connecting() {
        assertEquals(
            "Verbinden",
            ConnectionStatusTexts.summary(listOf(ConnectionPhase.CONNECTING))
        )
        assertEquals(
            "2 von 2 Verbindungen verbinden",
            ConnectionStatusTexts.summary(listOf(ConnectionPhase.CONNECTING, ConnectionPhase.CONNECTING))
        )
    }

    /** Ein Verbindungsabbruch nach Erfolg wird als Reconnect dargestellt. */
    @Test
    fun summary_reconnecting() {
        assertEquals(
            "Reconnect",
            ConnectionStatusTexts.summary(listOf(ConnectionPhase.RECONNECTING))
        )
        assertEquals(
            "1 aktiv, 1 Reconnect",
            ConnectionStatusTexts.summary(listOf(ConnectionPhase.ACTIVE, ConnectionPhase.RECONNECTING))
        )
    }

    /** Gemischte Phasen werden aufgezählt. */
    @Test
    fun summary_mixedPhases() {
        assertEquals(
            "1 aktiv, 1 Verbinden",
            ConnectionStatusTexts.summary(listOf(ConnectionPhase.ACTIVE, ConnectionPhase.CONNECTING))
        )
    }

    /** Phasenlabels sind kurz und fest. */
    @Test
    fun phaseLabel_returnsFixedNames() {
        assertEquals("Offline", ConnectionStatusTexts.phaseLabel(ConnectionPhase.OFFLINE))
        assertEquals("Verbinden", ConnectionStatusTexts.phaseLabel(ConnectionPhase.CONNECTING))
        assertEquals("Aktiv", ConnectionStatusTexts.phaseLabel(ConnectionPhase.ACTIVE))
        assertEquals("Reconnect", ConnectionStatusTexts.phaseLabel(ConnectionPhase.RECONNECTING))
    }

    /** Phasenmeldungen enthalten den Host, wenn vorhanden. */
    @Test
    fun phaseMessage_includesHost() {
        assertEquals("Offline", ConnectionStatusTexts.phaseMessage(ConnectionPhase.OFFLINE, "x"))
        assertEquals(
            "Verbinden mit mqtt.local:1883",
            ConnectionStatusTexts.phaseMessage(ConnectionPhase.CONNECTING, "mqtt.local:1883")
        )
        assertEquals(
            "Aktiv: mqtt.local:1883",
            ConnectionStatusTexts.phaseMessage(ConnectionPhase.ACTIVE, "mqtt.local:1883")
        )
        assertEquals(
            "Reconnect zu mqtt.local:1883",
            ConnectionStatusTexts.phaseMessage(ConnectionPhase.RECONNECTING, "mqtt.local:1883")
        )
    }

    /** Die Warteanzeige enthält die Restsekunden. */
    @Test
    fun waitingMessage_appendsRemainingSeconds() {
        assertEquals(
            "Verbinden mit mqtt.local:1883 (warten...5s)",
            ConnectionStatusTexts.waitingMessage(
                ConnectionPhase.CONNECTING,
                "mqtt.local:1883",
                5
            )
        )
        assertEquals(
            "Reconnect zu mqtt.local:1883 (warten...12s)",
            ConnectionStatusTexts.waitingMessage(
                ConnectionPhase.RECONNECTING,
                "mqtt.local:1883",
                12
            )
        )
    }

    /** Der Erstversuch-Fehler ist eindeutig formuliert. */
    @Test
    fun noConnectionPossible_includesHost() {
        assertEquals("Keine Verbindung möglich", ConnectionStatusTexts.noConnectionPossible())
        assertEquals(
            "Keine Verbindung möglich zu mqtt.local:1883",
            ConnectionStatusTexts.noConnectionPossible("mqtt.local:1883")
        )
    }

    /** Ausgeschaltete Verbindungen gelten als Offline. */
    @Test
    fun fromRuntime_disabledIsOffline() {
        assertEquals(
            ConnectionPhase.OFFLINE,
            ConnectionPhase.fromRuntime(isEnabled = false, status = "SUBSCRIBED")
        )
    }

    /** Eingeschaltete Verbindungen ohne Status gelten als Verbinden. */
    @Test
    fun fromRuntime_enabledWithoutStatusIsConnecting() {
        assertEquals(
            ConnectionPhase.CONNECTING,
            ConnectionPhase.fromRuntime(isEnabled = true, status = null)
        )
    }

    /** Statuscodes werden den vier Phasen zugeordnet. */
    @Test
    fun fromStatus_mapsKnownCodes() {
        assertEquals(ConnectionPhase.CONNECTING, ConnectionPhase.fromStatus("CONNECTING"))
        assertEquals(ConnectionPhase.RECONNECTING, ConnectionPhase.fromStatus("RECONNECTING"))
        assertEquals(ConnectionPhase.ACTIVE, ConnectionPhase.fromStatus("SUBSCRIBED"))
        assertEquals(ConnectionPhase.CONNECTING, ConnectionPhase.fromStatus("CONNECTED"))
        assertEquals(ConnectionPhase.OFFLINE, ConnectionPhase.fromStatus("OFFLINE"))
        assertEquals(ConnectionPhase.OFFLINE, ConnectionPhase.fromStatus("ERROR"))
    }

    /** Erstversuch-Fehler erhalten einen klaren Titel. */
    @Test
    fun errorTitle_detectsNoConnectionPossible() {
        assertEquals(
            "Keine Verbindung möglich",
            ConnectionStatusTexts.errorTitle("Keine Verbindung möglich zu mqtt.local:1883")
        )
    }

    /** Erreichbarkeitsfehler erhalten einen kurzen Notification-Titel. */
    @Test
    fun errorTitle_detectsUnreachableServer() {
        val message = "Verbindungsfehler: Server 'tcp://mqtt.local:1883' nicht erreichbar (UnknownHost)"
        assertEquals("Server nicht erreichbar", ConnectionStatusTexts.errorTitle(message))
    }

    /** Auth-Fehler werden als Anmeldungsproblem betitelt. */
    @Test
    fun errorTitle_detectsAuthFailure() {
        assertEquals(
            "Anmeldung fehlgeschlagen",
            ConnectionStatusTexts.errorTitle("Verbindung host:1883 - Falscher Benutzername oder Passwort")
        )
    }

    /** Unbekannte Fehler fallen auf den allgemeinen Titel zurück. */
    @Test
    fun errorTitle_fallsBackToGeneric() {
        assertEquals("Verbindungsfehler", ConnectionStatusTexts.errorTitle("Unerwarteter MQTT-Fehler"))
    }
}
