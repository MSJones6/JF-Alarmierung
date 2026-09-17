package de.msjones.android.alarmapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Gespeicherte MQTT-Serververbindung.
 */
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
    /** Serialisiert die Verbindung als JSON-Objekt. */
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

        /** Liest Verbindungseinstellungen aus einem JSON-Objekt. */
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

        /**
         * Liest Verbindungseinstellungen aus einem QR-Code-JSON.
         *
         * @return Einstellungen oder null bei ungültigem Originator/JSON
         */
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

/**
 * Persistente Verwaltung der MQTT-Verbindungen (verschlüsselt) und des Verbindungsstatus.
 *
 * Prozessweiter Singleton: Service, Activity und ViewModel müssen dieselbe Instanz nutzen,
 * damit Status-Updates in der UI ankommen.
 */
class SettingsStore private constructor(context: Context) {

    private val appContext = context.applicationContext

    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        appContext,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val statusPrefs: SharedPreferences =
        appContext.getSharedPreferences(STATUS_PREFS, Context.MODE_PRIVATE)

    private val _connectionsFlow = MutableStateFlow<List<ServerSettings>>(emptyList())
    val flow: Flow<List<ServerSettings>> = _connectionsFlow.asStateFlow()

    private val _activeConnectionId =
        MutableStateFlow(encryptedPrefs.getString(KEY_ACTIVE_CONNECTION_ID, null))
    val activeConnectionId: Flow<String?> = _activeConnectionId.asStateFlow()

    private val _connectionStatus = MutableStateFlow(statusPrefs.getString(KEY_STATUS, null))
    val connectionStatus: Flow<String?> = _connectionStatus.asStateFlow()

    private val _connectionStatusMessage =
        MutableStateFlow(statusPrefs.getString(KEY_STATUS_MESSAGE, null))
    val connectionStatusMessage: Flow<String?> = _connectionStatusMessage.asStateFlow()

    private val _connectionStatusTimestamp =
        MutableStateFlow(statusPrefs.getString(KEY_STATUS_TIMESTAMP, null)?.toLongOrNull())
    val connectionStatusTimestamp: Flow<Long?> = _connectionStatusTimestamp.asStateFlow()

    init {
        loadConnections()
    }

    /** Lädt Verbindungen aus dem verschlüsselten Speicher in den Flow. */
    private fun loadConnections() {
        val connectionsJson = encryptedPrefs.getString(KEY_CONNECTIONS, "[]") ?: "[]"
        _connectionsFlow.value = parseConnections(connectionsJson)
    }

    /**
     * Platzhalter für frühere DataStore-Migrationen (nicht mehr benötigt).
     */
    suspend fun migrateIfNeeded() {
        // Verbindungen liegen bereits in EncryptedSharedPreferences.
    }

    /** Aktuelle ID der aktiven Verbindung oder null. */
    fun getActiveConnectionId(): String? = _activeConnectionId.value

    /** Parst eine JSON-Array-Zeichenkette in Verbindungseinstellungen. */
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

    /** Speichert oder aktualisiert eine Verbindung. */
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

        encryptedPrefs.edit().putString(KEY_CONNECTIONS, jsonArray.toString()).apply()
        _connectionsFlow.value = currentConnections
    }

    /** Löscht eine Verbindung anhand ihrer ID. */
    suspend fun deleteConnection(id: String) {
        val updatedConnections = _connectionsFlow.value.filter { it.id != id }
        val jsonArray = JSONArray()
        updatedConnections.forEach { jsonArray.put(it.toJson()) }

        encryptedPrefs.edit().putString(KEY_CONNECTIONS, jsonArray.toString()).apply()
        _connectionsFlow.value = updatedConnections

        if (getActiveConnectionId() == id) {
            clearActiveConnection()
        }
    }

    /** Setzt die aktive Verbindung. */
    suspend fun setActiveConnection(id: String) {
        encryptedPrefs.edit().putString(KEY_ACTIVE_CONNECTION_ID, id).apply()
        _activeConnectionId.value = id
    }

    /** Entfernt die Markierung der aktiven Verbindung. */
    suspend fun clearActiveConnection() {
        encryptedPrefs.edit().remove(KEY_ACTIVE_CONNECTION_ID).apply()
        _activeConnectionId.value = null
    }

    /** Speichert Status und Nachricht der MQTT-Verbindung. */
    suspend fun setConnectionStatus(status: String, message: String) {
        val timestamp = System.currentTimeMillis()
        statusPrefs.edit()
            .putString(KEY_STATUS, status)
            .putString(KEY_STATUS_MESSAGE, message)
            .putString(KEY_STATUS_TIMESTAMP, timestamp.toString())
            .apply()
        _connectionStatus.value = status
        _connectionStatusMessage.value = message
        _connectionStatusTimestamp.value = timestamp
    }

    suspend fun setConnected(message: String = "Verbunden") = setConnectionStatus("connected", message)
    suspend fun setDisconnected(message: String = "Getrennt") =
        setConnectionStatus("disconnected", message)

    suspend fun setConnectionError(message: String) = setConnectionStatus("error", message)

    /** Löscht den gespeicherten Verbindungsstatus. */
    suspend fun clearConnectionStatus() {
        statusPrefs.edit()
            .remove(KEY_STATUS)
            .remove(KEY_STATUS_MESSAGE)
            .remove(KEY_STATUS_TIMESTAMP)
            .apply()
        _connectionStatus.value = null
        _connectionStatusMessage.value = null
        _connectionStatusTimestamp.value = null
    }

    /** Legt eine Standardverbindung an, falls noch keine existiert. */
    suspend fun initializeDefaultIfEmpty(defaultSettings: ServerSettings) {
        if (_connectionsFlow.value.isEmpty()) {
            saveConnection(defaultSettings)
        }
    }

    /** Liefert eine Momentaufnahme aller gespeicherten Verbindungen. */
    fun getConnectionsSnapshot(): List<ServerSettings> = _connectionsFlow.value

    companion object {
        private const val STATUS_PREFS = "connection_status_prefs"
        private const val KEY_CONNECTIONS = "connections"
        private const val KEY_ACTIVE_CONNECTION_ID = "active_connection_id"
        private const val KEY_STATUS = "connection_status"
        private const val KEY_STATUS_MESSAGE = "connection_status_message"
        private const val KEY_STATUS_TIMESTAMP = "connection_status_timestamp"

        @Volatile
        private var instance: SettingsStore? = null

        /**
         * Liefert die gemeinsame SettingsStore-Instanz für den gesamten Prozess.
         *
         * @param context beliebiger Context (wird auf ApplicationContext normalisiert)
         */
        fun getInstance(context: Context): SettingsStore {
            return instance ?: synchronized(this) {
                instance ?: SettingsStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
