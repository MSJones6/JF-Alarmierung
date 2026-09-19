package de.msjones.android.alarmapp.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests für die Unterdrückung kurz hintereinander eintreffender Doppelalarme.
 */
class IncomingAlarmDeduperTest {

    /** Dieselbe Payload innerhalb des Fensters wird nur einmal angenommen. */
    @Test
    fun accept_rejectsDuplicateInsideWindow() {
        var now = 1_000L
        val deduper = IncomingAlarmDeduper(windowMs = 2_000L, nowMs = { now })

        assertTrue(deduper.accept("Feueralarm###Gebäude 3###Rauch"))
        now = 1_500L
        assertFalse(deduper.accept("Feueralarm###Gebäude 3###Rauch"))
    }

    /** Nach Ablauf des Fensters gilt dieselbe Payload wieder als neuer Alarm. */
    @Test
    fun accept_allowsSamePayloadAfterWindow() {
        var now = 1_000L
        val deduper = IncomingAlarmDeduper(windowMs = 2_000L, nowMs = { now })

        assertTrue(deduper.accept("Feueralarm###Gebäude 3###Rauch"))
        now = 3_100L
        assertTrue(deduper.accept("Feueralarm###Gebäude 3###Rauch"))
    }

    /** Unterschiedliche Payloads werden unabhängig vom Zeitfenster angenommen. */
    @Test
    fun accept_allowsDifferentPayloadImmediately() {
        val deduper = IncomingAlarmDeduper(windowMs = 2_000L, nowMs = { 1_000L })

        assertTrue(deduper.accept("Feueralarm###Gebäude 3###Rauch"))
        assertTrue(deduper.accept("Warnung###IT-Systeme###Wartung"))
    }
}
