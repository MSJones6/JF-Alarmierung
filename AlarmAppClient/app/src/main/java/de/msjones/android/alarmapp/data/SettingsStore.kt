package de.msjones.android.alarmapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore by preferencesDataStore("settings")

data class ServerSettings(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val host: String = "",
    val port: Int = 1883,
    val username: String = "",
    val password: String = "",
    val topic: String = "JF/Alarm",
    val isActive: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("host", host)
            put("port", port)
            put("username", username)
            put("password", password)
            put("topic", topic)
            put("isActive", isActive)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ServerSettings {
            return ServerSettings(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", ""),
                host = json.optString("host", ""),
                port = json.optInt("port", 1883),
                username = json.optString("username", ""),
                password = json.optString("password", ""),
                topic = json.optString("topic", "JF/Alarm"),
                isActive = json.optBoolean("isActive", false)
            )
        }
    }
}

object SettingsKeys {
    val CONNECTIONS = stringPreferencesKey("connections")
    val ACTIVE_CONNECTION_ID = stringPreferencesKey("active_connection_id")
}

class SettingsStore(private val context: Context) {

    val flow: Flow<List<ServerSettings>> = context.dataStore.data.map { prefs: Preferences ->
        val connectionsJson = prefs[SettingsKeys.CONNECTIONS] ?: "[]"
        parseConnections(connectionsJson)
    }

    val activeConnectionId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SettingsKeys.ACTIVE_CONNECTION_ID]
    }

    private fun parseConnections(json: String): List<ServerSettings> {
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                ServerSettings.fromJson(jsonArray.getJSONObject(i))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveConnection(settings: ServerSettings) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[SettingsKeys.CONNECTIONS] ?: "[]"
            val connections = parseConnections(currentJson).toMutableList()

            val existingIndex = connections.indexOfFirst { it.id == settings.id }
            if (existingIndex >= 0) {
                connections[existingIndex] = settings
            } else {
                connections.add(settings)
            }

            val jsonArray = JSONArray()
            connections.forEach { jsonArray.put(it.toJson()) }
            prefs[SettingsKeys.CONNECTIONS] = jsonArray.toString()
        }
    }

    suspend fun deleteConnection(id: String) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[SettingsKeys.CONNECTIONS] ?: "[]"
            val connections = parseConnections(currentJson).filter { it.id != id }

            val jsonArray = JSONArray()
            connections.forEach { jsonArray.put(it.toJson()) }
            prefs[SettingsKeys.CONNECTIONS] = jsonArray.toString()
        }
    }

    suspend fun setActiveConnection(id: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.ACTIVE_CONNECTION_ID] = id
        }
    }

    suspend fun clearActiveConnection() {
        context.dataStore.edit { prefs ->
            prefs.remove(SettingsKeys.ACTIVE_CONNECTION_ID)
        }
    }

    suspend fun initializeDefaultIfEmpty(defaultSettings: ServerSettings) {
        context.dataStore.edit { prefs ->
            val currentJson = prefs[SettingsKeys.CONNECTIONS] ?: "[]"
            val connections = parseConnections(currentJson)
            if (connections.isEmpty()) {
                val jsonArray = JSONArray()
                jsonArray.put(defaultSettings.toJson())
                prefs[SettingsKeys.CONNECTIONS] = jsonArray.toString()
            }
        }
    }
}
