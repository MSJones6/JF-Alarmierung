package de.msjones.android.alarmapp.util

/**
 * Erkennt echte MQTT-Authentifizierungsfehler anhand von ConnAck-Meldungen.
 */
object ConnectionAuthDetector {

    /**
     * Prüft, ob ein Fehlertext einen abgelehnten Login beschreibt.
     *
     * Netzabbrüche und Timeouts gelten nicht als Auth-Fehler, damit ein Reconnect
     * nach Broker-Neustart nicht den Schalter zurücksetzt.
     *
     * @param message Fehlertext oder null
     * @return true nur bei klarem Benutzer/Passwort- oder Not-Authorized-ConnAck
     */
    fun isAuthFailure(message: String?): Boolean {
        val msgLower = message.orEmpty().lowercase()
        if (msgLower.isBlank() || isConnectionDrop(msgLower)) {
            return false
        }
        return hasAuthConnAck(msgLower)
    }

    /**
     * Erkennt ConnAck-Codes für abgelehnte Anmeldung.
     *
     * @param message bereits kleingeschriebener Fehlertext
     * @return true bei bekannten Auth-ConnAck-Texten
     */
    private fun hasAuthConnAck(message: String): Boolean {
        return message.contains("bad_user_name_or_password") ||
            message.contains("bad user name or password") ||
            message.contains("bad username or password") ||
            message.contains("not_authorized") ||
            message.contains("not authorized") ||
            message.contains("authentication failed")
    }

    /**
     * Erkennt Verbindungsabbrüche, die kein Anmeldeproblem sind.
     *
     * @param message bereits kleingeschriebener Fehlertext
     * @return true bei Netz- oder Kanalabbruch
     */
    private fun isConnectionDrop(message: String): Boolean {
        return message.contains("closed") ||
            message.contains("without disconnect") ||
            message.contains("connection refused") ||
            message.contains("connectexception") ||
            message.contains("timeout") ||
            message.contains("unreachable") ||
            message.contains("unknown host") ||
            message.contains("connection reset") ||
            message.contains("broken pipe") ||
            message.contains("eof")
    }
}
