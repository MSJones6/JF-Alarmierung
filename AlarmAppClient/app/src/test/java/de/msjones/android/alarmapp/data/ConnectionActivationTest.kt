package de.msjones.android.alarmapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit-Tests für [ConnectionActivation].
 */
class ConnectionActivationTest {

    /** Nur aktivierte Verbindungen mit Host werden für den Dienst ausgewählt. */
    @Test
    fun enabledForService_filtersInactiveAndBlankHost() {
        val enabled = ServerSettings(id = "a", host = "broker.local", isActive = true)
        val inactive = ServerSettings(id = "b", host = "other.local", isActive = false)
        val blankHost = ServerSettings(id = "c", host = "  ", isActive = true)

        val result = ConnectionActivation.enabledForService(listOf(enabled, inactive, blankHost))

        assertEquals(listOf(enabled), result)
    }

    /** Der Aktiv-Status einer Verbindung ändert sich unabhängig von den übrigen. */
    @Test
    fun setEnabled_updatesOnlyMatchingConnection() {
        val first = ServerSettings(id = "a", host = "one", isActive = false)
        val second = ServerSettings(id = "b", host = "two", isActive = true)

        val enabled = ConnectionActivation.setEnabled(listOf(first, second), "a", true)
        assertTrue(enabled.first { it.id == "a" }.isActive)
        assertTrue(enabled.first { it.id == "b" }.isActive)

        val disabled = ConnectionActivation.setEnabled(enabled, "b", false)
        assertTrue(disabled.first { it.id == "a" }.isActive)
        assertFalse(disabled.first { it.id == "b" }.isActive)
    }

    /** Unbekannte IDs lassen die Liste unverändert. */
    @Test
    fun setEnabled_ignoresUnknownId() {
        val connection = ServerSettings(id = "a", host = "one", isActive = false)
        val result = ConnectionActivation.setEnabled(listOf(connection), "missing", true)
        assertEquals(listOf(connection), result)
    }

    /** Alle Verbindungen werden deaktiviert. */
    @Test
    fun disableAll_clearsActiveFlags() {
        val connections = listOf(
            ServerSettings(id = "a", isActive = true),
            ServerSettings(id = "b", isActive = false)
        )
        val result = ConnectionActivation.disableAll(connections)
        assertTrue(result.none { it.isActive })
        assertEquals(2, result.size)
    }

    /** Gleiches Topic auf einem anderen Host ist erlaubt. */
    @Test
    fun isDuplicateConnection_allowsSameTopicOnDifferentHost() {
        val existing = ServerSettings(id = "a", host = "broker-a", port = 1883, topic = "JF/Alarm/#")
        val duplicate = ConnectionActivation.isDuplicateConnection(
            connections = listOf(existing),
            host = "broker-b",
            port = 1883,
            topic = "JF/Alarm/#"
        )
        assertFalse(duplicate)
    }

    /** Gleiches Topic auf einem anderen Port ist erlaubt. */
    @Test
    fun isDuplicateConnection_allowsSameTopicOnDifferentPort() {
        val existing = ServerSettings(id = "a", host = "broker-a", port = 1883, topic = "JF/Alarm/#")
        val duplicate = ConnectionActivation.isDuplicateConnection(
            connections = listOf(existing),
            host = "broker-a",
            port = 8883,
            topic = "JF/Alarm/#"
        )
        assertFalse(duplicate)
    }

    /** Host, Port und Topic zusammen gelten als Duplikat. */
    @Test
    fun isDuplicateConnection_rejectsSameHostPortAndTopic() {
        val existing = ServerSettings(id = "a", host = "Broker.Local", port = 1883, topic = "JF/Alarm/#")
        val duplicate = ConnectionActivation.isDuplicateConnection(
            connections = listOf(existing),
            host = "broker.local",
            port = 1883,
            topic = "jf/alarm/#"
        )
        assertTrue(duplicate)
    }

    /** Beim Bearbeiten zählt die eigene Verbindung nicht als Duplikat. */
    @Test
    fun isDuplicateConnection_ignoresConnectionBeingEdited() {
        val existing = ServerSettings(id = "a", host = "broker-a", port = 1883, topic = "JF/Alarm/#")
        val duplicate = ConnectionActivation.isDuplicateConnection(
            connections = listOf(existing),
            host = "broker-a",
            port = 1883,
            topic = "JF/Alarm/#",
            excludeId = "a"
        )
        assertFalse(duplicate)
    }
}

/**
 * Unit-Tests für die JSON-Serialisierung von [ServerSettings].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class ServerSettingsJsonTest {

    /** Aktiv-Status bleibt beim JSON-Rundlauf erhalten. */
    @Test
    fun toJsonAndFromJson_preservesIsActive() {
        val original = ServerSettings(
            id = "id-1",
            name = "Wache",
            host = "mqtt.local",
            port = 8883,
            username = "user",
            password = "secret",
            topic = "JF/Alarm/#",
            isActive = true,
            ssl = true
        )

        val restored = ServerSettings.fromJson(original.toJson())

        assertEquals(original, restored)
    }

    /** Fehlendes isActive-Feld im JSON gilt als deaktiviert. */
    @Test
    fun fromJson_defaultsIsActiveToFalse() {
        val json = originalJsonWithoutIsActive()
        val restored = ServerSettings.fromJson(json)
        assertFalse(restored.isActive)
    }

    /**
     * Erzeugt ein JSON-Objekt ohne isActive-Feld.
     *
     * @return JSON ohne Aktiv-Status
     */
    private fun originalJsonWithoutIsActive(): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put("id", "id-2")
            put("host", "mqtt.local")
            put("port", 1883)
        }
    }
}
