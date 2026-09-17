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

    /**
     * Prüft, ob bereits eine Verbindung mit gleichem Host, Port und Topic existiert.
     *
     * @param connections gespeicherte Verbindungen
     * @param host Broker-Host
     * @param port Broker-Port
     * @param topic MQTT-Topic
     * @param excludeId Kennung der gerade bearbeiteten Verbindung oder null
     * @return true bei identischer Kombination aus Host, Port und Topic
     */
    fun isDuplicateConnection(
        connections: List<ServerSettings>,
        host: String,
        port: Int,
        topic: String,
        excludeId: String? = null
    ): Boolean {
        val trimmedHost = host.trim()
        val trimmedTopic = topic.trim()
        return connections.any { connection ->
            connection.id != excludeId &&
                connection.host.equals(trimmedHost, ignoreCase = true) &&
                connection.port == port &&
                connection.topic.equals(trimmedTopic, ignoreCase = true)
        }
    }
}
