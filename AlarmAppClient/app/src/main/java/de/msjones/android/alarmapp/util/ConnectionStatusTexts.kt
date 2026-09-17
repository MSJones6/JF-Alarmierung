package de.msjones.android.alarmapp.util

/**
 * Texte für Verbindungsstatus in UI und Android-Benachrichtigungen.
 */
object ConnectionStatusTexts {

    /**
     * Liefert die Kurzfassung über alle Verbindungsphasen.
     *
     * @param phases aktuelle Phase je gespeicherter Verbindung
     * @return Statuszeile für Notification und Einstellungen
     */
    fun summary(phases: Collection<ConnectionPhase>): String {
        val total = phases.size
        val active = phases.count { it == ConnectionPhase.ACTIVE }
        val connecting = phases.count { it == ConnectionPhase.CONNECTING }
        val reconnecting = phases.count { it == ConnectionPhase.RECONNECTING }

        if (total == 0 || (active == 0 && connecting == 0 && reconnecting == 0)) {
            return "Offline"
        }
        if (connecting == 0 && reconnecting == 0) {
            return "$active von $total Verbindungen aktiv"
        }
        if (active == 0 && reconnecting == 0) {
            return if (connecting == 1 && total == 1) {
                "Verbinden"
            } else {
                "$connecting von $total Verbindungen verbinden"
            }
        }
        if (active == 0 && connecting == 0) {
            return if (reconnecting == 1 && total == 1) {
                "Reconnect"
            } else {
                "$reconnecting von $total Verbindungen Reconnect"
            }
        }

        val parts = mutableListOf<String>()
        if (active > 0) {
            parts += "$active aktiv"
        }
        if (connecting > 0) {
            parts += "$connecting Verbinden"
        }
        if (reconnecting > 0) {
            parts += "$reconnecting Reconnect"
        }
        return parts.joinToString(", ")
    }

    /**
     * Kurzes Phasenlabel ohne Host.
     *
     * @param phase aktuelle Verbindungsphase
     * @return Offline, Verbinden, Aktiv oder Reconnect
     */
    fun phaseLabel(phase: ConnectionPhase): String {
        return when (phase) {
            ConnectionPhase.OFFLINE -> "Offline"
            ConnectionPhase.CONNECTING -> "Verbinden"
            ConnectionPhase.ACTIVE -> "Aktiv"
            ConnectionPhase.RECONNECTING -> "Reconnect"
        }
    }

    /**
     * Ausführliche Phasenmeldung, optional mit Host.
     *
     * @param phase aktuelle Verbindungsphase
     * @param host Anzeigename des Brokers
     * @return Text für Notification und Statuszeile
     */
    fun phaseMessage(phase: ConnectionPhase, host: String = ""): String {
        val target = host.trim()
        return when (phase) {
            ConnectionPhase.OFFLINE -> "Offline"
            ConnectionPhase.CONNECTING ->
                if (target.isEmpty()) "Verbinden" else "Verbinden mit $target"
            ConnectionPhase.ACTIVE ->
                if (target.isEmpty()) "Aktiv" else "Aktiv: $target"
            ConnectionPhase.RECONNECTING ->
                if (target.isEmpty()) "Reconnect" else "Reconnect zu $target"
        }
    }

    /**
     * Wählt einen kurzen Notification-Titel anhand der Fehlermeldung.
     *
     * @param message ausführliche Status- oder Fehlermeldung
     * @return Titelzeile für die Status-Notification
     */
    fun errorTitle(message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("nicht erreichbar") -> "Server nicht erreichbar"
            lower.contains("passwort") || lower.contains("benutzername") -> "Anmeldung fehlgeschlagen"
            lower.contains("zeitüberschreitung") -> "Zeitüberschreitung"
            else -> "Verbindungsfehler"
        }
    }
}
