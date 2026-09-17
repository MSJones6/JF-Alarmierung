package de.msjones.android.alarmapp.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Persistierte Alarmnachricht für die lokale Historie.
 */
data class AlarmMessage(
    val id: String,
    val keyword: String,
    val location: String,
    val extras: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Speichert und lädt Alarmnachrichten über SharedPreferences (ohne native DataStore-Libs).
 */
class MessageStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _messages = MutableStateFlow(loadMessages())

    /** Beobachtbare Liste aller gespeicherten Alarmnachrichten. */
    val flow: Flow<List<AlarmMessage>> = _messages.asStateFlow()

    /** Fügt eine neue Alarmnachricht am Anfang der Historie hinzu. */
    suspend fun addMessage(keyword: String, location: String, extras: String) {
        val newMessage = AlarmMessage(
            id = System.currentTimeMillis().toString(),
            keyword = keyword,
            location = location,
            extras = extras
        )
        _messages.update { current ->
            val updated = listOf(newMessage) + current
            persist(updated)
            updated
        }
    }

    /** Entfernt eine Nachricht anhand ihrer ID. */
    suspend fun removeMessage(id: String) {
        _messages.update { current ->
            val updated = current.filter { it.id != id }
            persist(updated)
            updated
        }
    }

    /** Löscht die gesamte Nachrichtenhistorie. */
    suspend fun clearAllMessages() {
        persist(emptyList())
        _messages.value = emptyList()
    }

    /** Lädt Nachrichten aus SharedPreferences. */
    private fun loadMessages(): List<AlarmMessage> {
        val json = prefs.getString(KEY_MESSAGES, "").orEmpty()
        return if (json.isEmpty()) emptyList() else parseMessages(json)
    }

    /** Schreibt die Nachrichtenliste persistent. */
    private fun persist(messages: List<AlarmMessage>) {
        prefs.edit().putString(KEY_MESSAGES, serializeMessages(messages)).apply()
    }

    private fun parseMessages(json: String): List<AlarmMessage> {
        return try {
            json.split("|||").filter { it.isNotEmpty() }.mapNotNull { entry ->
                val parts = entry.split("###", limit = 5)
                if (parts.size >= 5) {
                    AlarmMessage(
                        id = parts[0],
                        keyword = parts[1],
                        location = parts[2],
                        extras = parts[3],
                        timestamp = parts[4].toLongOrNull() ?: System.currentTimeMillis()
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeMessages(messages: List<AlarmMessage>): String {
        return messages.joinToString("|||") { msg ->
            "${msg.id}###${msg.keyword}###${msg.location}###${msg.extras}###${msg.timestamp}"
        }
    }

    companion object {
        private const val PREFS_NAME = "messages"
        private const val KEY_MESSAGES = "messages"
    }
}
