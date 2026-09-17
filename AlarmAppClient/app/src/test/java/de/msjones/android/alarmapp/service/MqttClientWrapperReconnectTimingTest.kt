package de.msjones.android.alarmapp.service

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit-Tests für die konfigurierten Reconnect-Pausen.
 */
class MqttClientWrapperReconnectTimingTest {

    /** Der erste Reconnect erfolgt nach einer Sekunde. */
    @Test
    fun reconnectInitialDelay_isOneSecond() {
        assertEquals(1L, MqttClientWrapper.RECONNECT_INITIAL_DELAY_SECONDS)
    }

    /** Die Pause zwischen Reconnects ist auf 30 Sekunden begrenzt. */
    @Test
    fun reconnectMaxDelay_isThirtySeconds() {
        assertEquals(30L, MqttClientWrapper.RECONNECT_MAX_DELAY_SECONDS)
    }
}
