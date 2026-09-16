package de.msjones.android.alarmapp.data

/**
 * Parst das MQTT-Payload-Format der JF-Alarmierung.
 *
 * Erwartetes Format: `Alarmstichwort###Ort###Sonstiges`
 */
object AlarmMessageParser {

    /**
     * Zerlegt eine Rohnachricht in Stichwort, Ort und Zusatzinformationen.
     *
     * @param rawMessage unveränderte MQTT-Payload als UTF-8-Text
     * @return strukturierte Alarmfelder
     */
    fun parse(rawMessage: String): ParsedAlarmMessage {
        val parts = rawMessage.split("###", limit = 3)
        return ParsedAlarmMessage(
            keyword = parts.getOrNull(0)?.trim().orEmpty(),
            location = parts.getOrNull(1)?.trim().orEmpty(),
            extras = parts.getOrNull(2)?.trim().orEmpty()
        )
    }
}

/**
 * Strukturierte Darstellung einer geparsten Alarmnachricht.
 */
data class ParsedAlarmMessage(
    val keyword: String,
    val location: String,
    val extras: String
) {
    /** Prüft, ob mindestens ein Feld Inhalt hat. */
    fun isNotBlank(): Boolean =
        keyword.isNotBlank() || location.isNotBlank() || extras.isNotBlank()
}
