package de.msjones.android.alarmapp.util

import de.msjones.android.alarmapp.data.ServerSettings

/**
 * Texte für Verbindungsstatus in UI und Android-Benachrichtigungen.
 */
object ConnectionStatusTexts {

    /**
     * Ermittelt die Phase jeder gespeicherten Verbindung.
     *
     * @param connections gespeicherte Verbindungen
     * @param runtimeStatuses letzter Statuscode je Verbindungs-ID
     * @return Phasen in derselben Reihenfolge wie [connections]
     */
    fun phasesFor(
        connections: List<ServerSettings>,
        runtimeStatuses: Map<String, String>
    ): List<ConnectionPhase> {
        return connections.map { connection ->
            ConnectionPhase.fromRuntime(connection.isActive, runtimeStatuses[connection.id])
        }
    }

    /**
     * Liefert die Statuszeile über alle Verbindungen, inkl. einzelner Warteanzeige.
     *
     * @param connections gespeicherte Verbindungen
     * @param runtimeStatuses letzter Statuscode je Verbindungs-ID
     * @param runtimeMessages letzter Anzeigetext je Verbindungs-ID
     * @return Text für Nachrichtenseite, Einstellungen und Notification
     */
    fun displaySummary(
        connections: List<ServerSettings>,
        runtimeStatuses: Map<String, String>,
        runtimeMessages: Map<String, String> = emptyMap()
    ): String {
        val waitingMessages = connections.mapNotNull { connection ->
            val phase = ConnectionPhase.fromRuntime(connection.isActive, runtimeStatuses[connection.id])
            val message = runtimeMessages[connection.id]
            if ((phase == ConnectionPhase.CONNECTING || phase == ConnectionPhase.RECONNECTING) &&
                !message.isNullOrBlank()
            ) {
                message
            } else {
                null
            }
        }
        return if (waitingMessages.size == 1) {
            waitingMessages.first()
        } else {
            summary(phasesFor(connections, runtimeStatuses))
        }
    }

    /**
     * Verdichtet alle Phasen zu einer Anzeigefarbe bzw. einem Gesamtlage-Status.
     *
     * Eine aktive Verbindung hält das Gesamtergebnis auf Aktiv, auch wenn andere offline sind.
     *
     * @param phases Phasen aller gespeicherten Verbindungen
     * @return übergeordnete Phase für die Nachrichtenseite
     */
    fun overallPhase(phases: Collection<ConnectionPhase>): ConnectionPhase {
        val active = phases.count { it == ConnectionPhase.ACTIVE }
        val connecting = phases.count { it == ConnectionPhase.CONNECTING }
        val reconnecting = phases.count { it == ConnectionPhase.RECONNECTING }

        if (active == 0 && connecting == 0 && reconnecting == 0) {
            return ConnectionPhase.OFFLINE
        }
        if (connecting == 0 && reconnecting == 0) {
            return ConnectionPhase.ACTIVE
        }
        if (active == 0 && reconnecting == 0) {
            return ConnectionPhase.CONNECTING
        }
        if (active == 0 && connecting == 0) {
            return ConnectionPhase.RECONNECTING
        }
        return if (active > 0) {
            ConnectionPhase.ACTIVE
        } else {
            ConnectionPhase.RECONNECTING
        }
    }

    /**
     * Liefert den Statuscode zur übergeordneten Phase.
     *
     * @param phase verdichtete Phase
     * @return Code analog zu den MQTT-Ereignissen
     */
    fun statusCode(phase: ConnectionPhase): String {
        return when (phase) {
            ConnectionPhase.OFFLINE -> "OFFLINE"
            ConnectionPhase.CONNECTING -> "CONNECTING"
            ConnectionPhase.ACTIVE -> "SUBSCRIBED"
            ConnectionPhase.RECONNECTING -> "RECONNECTING"
        }
    }

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
     * Phasenmeldung inklusive Restwartezeit.
     *
     * @param phase aktuelle Verbindungsphase
     * @param host Anzeigename des Brokers
     * @param remainingSeconds noch zu wartende Sekunden
     * @return z. B. „Verbinden mit host (warten...5s)“
     */
    fun waitingMessage(phase: ConnectionPhase, host: String, remainingSeconds: Int): String {
        val seconds = remainingSeconds.coerceAtLeast(0)
        return "${phaseMessage(phase, host)} (warten...${seconds}s)"
    }

    /**
     * Meldung, wenn der erste Verbindungsversuch scheitert.
     *
     * @param host Anzeigename des Brokers
     * @return Fehlertext für Notification und Snackbar
     */
    fun noConnectionPossible(host: String = ""): String {
        val target = host.trim()
        return if (target.isEmpty()) {
            "Keine Verbindung möglich"
        } else {
            "Keine Verbindung möglich zu $target"
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
            lower.contains("keine verbindung") -> "Keine Verbindung möglich"
            lower.contains("nicht erreichbar") -> "Server nicht erreichbar"
            lower.contains("passwort") || lower.contains("benutzername") -> "Anmeldung fehlgeschlagen"
            lower.contains("zeitüberschreitung") -> "Zeitüberschreitung"
            else -> "Verbindungsfehler"
        }
    }
}
