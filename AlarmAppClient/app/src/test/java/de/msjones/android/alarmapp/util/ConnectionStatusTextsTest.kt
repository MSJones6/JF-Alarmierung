package de.msjones.android.alarmapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit-Tests für [ConnectionStatusTexts].
 */
class ConnectionStatusTextsTest {

    /** Aktive und gespeicherte Verbindungen werden als Verhältnis ausgegeben. */
    @Test
    fun summary_formatsEnabledOfTotal() {
        assertEquals("1 von 2 Verbindungen aktiv", ConnectionStatusTexts.summary(1, 2))
        assertEquals("2 von 2 Verbindungen aktiv", ConnectionStatusTexts.summary(2, 2))
    }

    /** Ohne aktive Verbindung erscheint der Leer-Hinweis. */
    @Test
    fun summary_withoutEnabledConnections() {
        assertEquals("Keine Verbindung aktiv", ConnectionStatusTexts.summary(0, 2))
        assertEquals("Keine Verbindung aktiv", ConnectionStatusTexts.summary(1, 0))
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
