package de.msjones.android.alarmapp.util

/**
 * Unterdrückt identische Alarm-Payloads, die innerhalb eines kurzen Zeitfensters
 * mehrfach vom Broker oder von gestapelten Subscribe-Callbacks eintreffen.
 */
class IncomingAlarmDeduper(
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() }
) {
    private var lastPayload: String? = null
    private var lastAcceptedAt: Long = 0L

    /**
     * Prüft, ob eine Roh-Nachricht als neuer Alarm behandelt werden soll.
     *
     * @param payload unveränderte MQTT-Payload
     * @return true, wenn die Nachricht neu ist und verarbeitet werden darf
     */
    fun accept(payload: String): Boolean {
        val now = nowMs()
        if (payload == lastPayload && now - lastAcceptedAt < windowMs) {
            return false
        }
        lastPayload = payload
        lastAcceptedAt = now
        return true
    }

    companion object {
        /** Zeitfenster, in dem dieselbe Payload als Duplikat gilt. */
        const val DEFAULT_WINDOW_MS = 2_000L
    }
}
