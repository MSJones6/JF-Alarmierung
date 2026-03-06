package de.msjones.android.alarmapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    val isActive: Boolean = false,
    val ssl: Boolean = false
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
            put("ssl", ssl)
        }
    }

    companion object {
        private const val VALID_ORIGINATOR = "MSJones JF Alarm App"

        fun fromJson(json: JSONObject): ServerSettings {
            return ServerSettings(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", ""),
                host = json.optString("host", ""),
                port = json.optInt("port", 1883),
                username = json.optString("username", ""),
                password = json.optString("password", ""),
                topic = json.optString("topic", "JF/Alarm"),
                isActive = json.optBoolean("isActive", false),
                ssl = json.optBoolean("ssl", false)
            )
        }

        fun fromQrCode(jsonString: String): ServerSettings? {
            return try {
                val json = JSONObject(jsonString)
                val originator = json.optString("originator", "")
                if (originator != VALID_ORIGINATOR) {
                    null
                } else {
                    ServerSettings(
                        id = UUID.randomUUID().toString(),
                        name = json.optString("name", ""),
                        host = json.optString("host", ""),
                        port = json.optInt("port", 1883),
                        username = json.optString("username", ""),
                        password = json.optString("password", ""),
                        topic = json.optString("topic", "JF/Alarm"),
                        isActive = false,
                        ssl = json.optBoolean("ssl", false)
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

object SettingsKeys {
    val CONNECTIONS = stringPreferencesKey("connections")
    val ACTIVE_CONNECTION_ID = stringPreferencesKey("active_connection_id")
    val CONNECTION_STATUS = stringPreferencesKey("connection_status")
    val CONNECTION_STATUS_MESSAGE = stringPreferencesKey("connection_status_message")
    val CONNECTION_STATUS_TIMESTAMP = stringPreferencesKey("connection_status_timestamp")
}

class SettingsStore(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _connectionsFlow = MutableStateFlow<List<ServerSettings>>(emptyList())
    val flow: Flow<List<ServerSettings>> = _connectionsFlow

    init {
        loadConnections()
    }

    private fun loadConnections() {
        val connectionsJson = encryptedPrefs.getString("connections", "[]") ?: "[]"
        _connectionsFlow.value = parseConnections(connectionsJson)
    }

    /**
     * Führt eine Migration vom alten DataStore zum neuen verschlüsselten Speicher durch.
     */
    suspend fun migrateIfNeeded() {
        val oldPrefs = context.dataStore.data.first()
        val oldConnectionsJson = oldPrefs[SettingsKeys.CONNECTIONS]

        if (oldConnectionsJson != null) {
            // In den neuen verschlüsselten Speicher schreiben
            encryptedPrefs.edit().putString("connections", oldConnectionsJson).apply()

            // Auch die aktive Connection ID migrieren
            val oldActiveId = oldPrefs[SettingsKeys.ACTIVE_CONNECTION_ID]
            if (oldActiveId != null) {
                encryptedPrefs.edit().putString("active_connection_id", oldActiveId).apply()
            }

            // Alten Speicher leeren
            context.dataStore.edit { it.clear() }

            // Daten neu laden
            loadConnections()
        }
    }

    val activeConnectionId: Flow<String?> = MutableStateFlow(encryptedPrefs.getString("active_connection_id", null)).apply {
        // In einer echten App würde man hier einen Listener auf SharedPreferences registrieren
    }

    fun getActiveConnectionId(): String? = encryptedPrefs.getString("active_connection_id", null)

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
        val currentConnections = _connectionsFlow.value.toMutableList()
        val existingIndex = currentConnections.indexOfFirst { it.id == settings.id }

        if (existingIndex >= 0) {
            currentConnections[existingIndex] = settings
        } else {
            currentConnections.add(settings)
        }

        val jsonArray = JSONArray()
        currentConnections.forEach { jsonArray.put(it.toJson()) }

        encryptedPrefs.edit().putString("connections", jsonArray.toString()).apply()
        _connectionsFlow.value = currentConnections
    }

    suspend fun deleteConnection(id: String) {
        val updatedConnections = _connectionsFlow.value.filter { it.id != id }
        val jsonArray = JSONArray()
        updatedConnections.forEach { jsonArray.put(it.toJson()) }

        encryptedPrefs.edit().putString("connections", jsonArray.toString()).apply()
        _connectionsFlow.value = updatedConnections

        if (getActiveConnectionId() == id) {
            clearActiveConnection()
        }
    }

    suspend fun setActiveConnection(id: String) {
        encryptedPrefs.edit().putString("active_connection_id", id).apply()
    }

    suspend fun clearActiveConnection() {
        encryptedPrefs.edit().remove("active_connection_id").apply()
    }

    // Status-Informationen bleiben im normalen DataStore (nicht sensibel)
    val connectionStatus: Flow<String?> = context.dataStore.data.map { it[SettingsKeys.CONNECTION_STATUS] }
    val connectionStatusMessage: Flow<String?> = context.dataStore.data.map { it[SettingsKeys.CONNECTION_STATUS_MESSAGE] }
    val connectionStatusTimestamp: Flow<Long?> = context.dataStore.data.map { it[SettingsKeys.CONNECTION_STATUS_TIMESTAMP]?.toLongOrNull() }

    suspend fun setConnectionStatus(status: String, message: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.CONNECTION_STATUS] = status
            prefs[SettingsKeys.CONNECTION_STATUS_MESSAGE] = message
            prefs[SettingsKeys.CONNECTION_STATUS_TIMESTAMP] = System.currentTimeMillis().toString()
        }
    }

    suspend fun setConnected(message: String = "Verbunden") = setConnectionStatus("connected", message)
    suspend fun setDisconnected(message: String = "Getrennt") = setConnectionStatus("disconnected", message)
    suspend fun setConnectionError(message: String) = setConnectionStatus("error", message)

    suspend fun clearConnectionStatus() {
        context.dataStore.edit { prefs ->
            prefs.remove(SettingsKeys.CONNECTION_STATUS)
            prefs.remove(SettingsKeys.CONNECTION_STATUS_MESSAGE)
            prefs.remove(SettingsKeys.CONNECTION_STATUS_TIMESTAMP)
        }
    }

    suspend fun initializeDefaultIfEmpty(defaultSettings: ServerSettings) {
        if (_connectionsFlow.value.isEmpty()) {
            saveConnection(defaultSettings)
        }
    }
}
