package de.msjones.android.alarmapp.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-Tests für [ConnectionAuthDetector].
 */
class ConnectionAuthDetectorTest {

    /** Ein ConnAck mit falschem Passwort gilt als Auth-Fehler. */
    @Test
    fun isAuthFailure_detectsBadCredentialsConnAck() {
        assertTrue(
            ConnectionAuthDetector.isAuthFailure("BAD_USER_NAME_OR_PASSWORD")
        )
        assertTrue(
            ConnectionAuthDetector.isAuthFailure("CONNACK contained an Error Code: NOT_AUTHORIZED")
        )
    }

    /** Ein Broker-Neustart bzw. Kanalabbruch ist kein Auth-Fehler. */
    @Test
    fun isAuthFailure_ignoresConnectionDrop() {
        assertFalse(
            ConnectionAuthDetector.isAuthFailure(
                "Server closed connection without DISCONNECT."
            )
        )
        assertFalse(
            ConnectionAuthDetector.isAuthFailure("Connection reset by peer")
        )
        assertFalse(
            ConnectionAuthDetector.isAuthFailure("identifier rejected")
        )
        assertFalse(
            ConnectionAuthDetector.isAuthFailure("Connection refused")
        )
        assertFalse(ConnectionAuthDetector.isAuthFailure(null))
        assertFalse(ConnectionAuthDetector.isAuthFailure(""))
    }
}
