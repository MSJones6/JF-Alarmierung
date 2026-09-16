package de.msjones.android.alarmapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit-Tests für [AlarmMessageParser].
 */
class AlarmMessageParserTest {

    /** Prüft das Standardformat mit drei Feldern. */
    @Test
    fun parse_splitsKeywordLocationAndExtras() {
        val result = AlarmMessageParser.parse("Brand###Hauptstraße 1###Keller")
        assertEquals("Brand", result.keyword)
        assertEquals("Hauptstraße 1", result.location)
        assertEquals("Keller", result.extras)
        assertTrue(result.isNotBlank())
    }

    /** Prüft, dass fehlende Felder leer bleiben. */
    @Test
    fun parse_handlesMissingParts() {
        val result = AlarmMessageParser.parse("Hilfe")
        assertEquals("Hilfe", result.keyword)
        assertEquals("", result.location)
        assertEquals("", result.extras)
    }

    /** Prüft Trim von Whitespace. */
    @Test
    fun parse_trimsWhitespace() {
        val result = AlarmMessageParser.parse("  Brand  ###  Ort  ###  Info  ")
        assertEquals("Brand", result.keyword)
        assertEquals("Ort", result.location)
        assertEquals("Info", result.extras)
    }

    /** Leere Nachricht gilt als blank. */
    @Test
    fun parse_emptyMessageIsBlank() {
        assertFalse(AlarmMessageParser.parse("").isNotBlank())
    }
}

/**
 * Unit-Tests für das QR-Code-JSON der Verbindungsanlage (benötigt Android-JSONObject).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class ServerSettingsQrCodeTest {

    /** Gültiger Originator liefert Einstellungen. */
    @Test
    fun fromQrCode_acceptsValidOriginator() {
        val json = """
            {
              "originator": "MSJones JF Alarm App",
              "name": "Test",
              "host": "mqtt.example",
              "port": 8883,
              "username": "user",
              "password": "secret",
              "topic": "JF/Alarm",
              "ssl": true
            }
        """.trimIndent()

        val settings = ServerSettings.fromQrCode(json)
        assertEquals("mqtt.example", settings?.host)
        assertEquals(8883, settings?.port)
        assertEquals("user", settings?.username)
        assertEquals("secret", settings?.password)
        assertEquals("JF/Alarm", settings?.topic)
        assertTrue(settings?.ssl == true)
    }

    /** Fremder Originator wird abgelehnt. */
    @Test
    fun fromQrCode_rejectsInvalidOriginator() {
        val json = """{"originator":"Other App","host":"x"}"""
        assertNull(ServerSettings.fromQrCode(json))
    }

    /** Ungültiges JSON liefert null. */
    @Test
    fun fromQrCode_rejectsInvalidJson() {
        assertNull(ServerSettings.fromQrCode("not-json"))
    }
}
