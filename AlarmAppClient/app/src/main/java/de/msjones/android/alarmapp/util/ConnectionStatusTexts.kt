package de.msjones.android.alarmapp.util

/**
 * Texte für Verbindungsstatus in UI und Android-Benachrichtigungen.
 */
object ConnectionStatusTexts {

    /**
     * Liefert die Kurzfassung, wie viele Verbindungen gerade aktiv sind.
     *
     * @param enabledCount Anzahl der vom Nutzer aktivierten Verbindungen
     * @param totalCount Anzahl aller gespeicherten Verbindungen
     * @return Statuszeile für Notification und Einstellungen
     */
    fun summary(enabledCount: Int, totalCount: Int): String {
        return if (enabledCount <= 0 || totalCount <= 0) {
            "Keine Verbindung aktiv"
        } else {
            "$enabledCount von $totalCount Verbindungen aktiv"
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
