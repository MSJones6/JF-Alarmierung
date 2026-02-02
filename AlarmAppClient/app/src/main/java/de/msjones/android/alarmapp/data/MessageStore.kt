package de.msjones.android.alarmapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.messageDataStore by preferencesDataStore("messages")

data class AlarmMessage(
    val id: String,
    val keyword: String,
    val location: String,
    val extras: String,
    val timestamp: Long = System.currentTimeMillis()
)

object MessageKeys {
    val MESSAGES = stringPreferencesKey("messages")
}

class MessageStore(private val context: Context) {

    val flow: Flow<List<AlarmMessage>> = context.messageDataStore.data.map { prefs: Preferences ->
        val messagesJson = prefs[MessageKeys.MESSAGES] ?: ""
        if (messagesJson.isEmpty()) {
            emptyList()
        } else {
            parseMessages(messagesJson)
        }
    }

    suspend fun addMessage(keyword: String, location: String, extras: String) {
        val newMessage = AlarmMessage(
            id = System.currentTimeMillis().toString(),
            keyword = keyword,
            location = location,
            extras = extras
        )

        context.messageDataStore.edit { prefs ->
            val currentJson = prefs[MessageKeys.MESSAGES] ?: ""
            val currentMessages = if (currentJson.isEmpty()) {
                emptyList()
            } else {
                parseMessages(currentJson)
            }
            val updatedMessages = listOf(newMessage) + currentMessages
            prefs[MessageKeys.MESSAGES] = serializeMessages(updatedMessages)
        }
    }

    suspend fun removeMessage(id: String) {
        context.messageDataStore.edit { prefs ->
            val currentJson = prefs[MessageKeys.MESSAGES] ?: ""
            if (currentJson.isNotEmpty()) {
                val currentMessages = parseMessages(currentJson)
                val updatedMessages = currentMessages.filter { it.id != id }
                prefs[MessageKeys.MESSAGES] = serializeMessages(updatedMessages)
            }
        }
    }

    suspend fun clearAllMessages() {
        context.messageDataStore.edit { prefs ->
            prefs[MessageKeys.MESSAGES] = ""
        }
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
}
