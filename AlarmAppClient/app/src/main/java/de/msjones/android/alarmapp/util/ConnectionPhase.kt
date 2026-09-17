package de.msjones.android.alarmapp.util

/**
 * Laufzeitphasen einer MQTT-Verbindung.
 */
enum class ConnectionPhase {
    OFFLINE,
    CONNECTING,
    ACTIVE,
    RECONNECTING;

    companion object {
        /**
         * Leitet die Phase aus Nutzer-Schalter und MQTT-Statuscode ab.
         *
         * @param isEnabled ob die Verbindung eingeschaltet ist
         * @param status letzter Statuscode oder null
         * @return passende [ConnectionPhase]
         */
        fun fromRuntime(isEnabled: Boolean, status: String?): ConnectionPhase {
            if (!isEnabled) {
                return OFFLINE
            }
            return fromStatus(status, enabledFallback = CONNECTING)
        }

        /**
         * Leitet die Phase aus einem MQTT-Statuscode ab.
         *
         * @param status Statuscode wie CONNECTING oder SUBSCRIBED
         * @param enabledFallback Phase, wenn der Schalter an ist, aber noch kein Status vorliegt
         * @return passende [ConnectionPhase]
         */
        fun fromStatus(
            status: String?,
            enabledFallback: ConnectionPhase = OFFLINE
        ): ConnectionPhase {
            return when (status?.uppercase()) {
                "CONNECTING" -> CONNECTING
                "RECONNECTING" -> RECONNECTING
                "CONNECTED" -> CONNECTING
                "SUBSCRIBED", "ACTIVE" -> ACTIVE
                "ERROR", "OFFLINE", "DISCONNECTED" -> OFFLINE
                null, "" -> enabledFallback
                else -> enabledFallback
            }
        }
    }
}
