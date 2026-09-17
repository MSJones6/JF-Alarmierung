package de.msjones.android.alarmapp.data

/**
 * Hilfsfunktionen zur unabhängigen Aktivierung einzelner MQTT-Verbindungen.
 */
object ConnectionActivation {

    /**
     * Liefert alle Verbindungen, die der Messaging-Dienst starten soll.
     *
     * @param connections alle gespeicherten Verbindungen
     * @return aktivierte Verbindungen mit gesetztem Host
     */
    fun enabledForService(connections: List<ServerSettings>): List<ServerSettings> {
        return connections.filter { it.isActive && it.host.isNotBlank() }
    }

    /**
     * Setzt den Aktiv-Status genau einer Verbindung und lässt die übrigen unverändert.
     *
     * @param connections aktuelle Verbindungsliste
     * @param id Kennung der zu ändernden Verbindung
     * @param enabled neuer Aktiv-Status
     * @return Kopie der Liste mit aktualisiertem Status
     */
    fun setEnabled(
        connections: List<ServerSettings>,
        id: String,
        enabled: Boolean
    ): List<ServerSettings> {
        return connections.map { connection ->
            if (connection.id == id) connection.copy(isActive = enabled) else connection
        }
    }

    /**
     * Deaktiviert alle Verbindungen.
     *
     * @param connections aktuelle Verbindungsliste
     * @return Kopie der Liste mit durchgängig deaktivierten Verbindungen
     */
    fun disableAll(connections: List<ServerSettings>): List<ServerSettings> {
        return connections.map { it.copy(isActive = false) }
    }
}
