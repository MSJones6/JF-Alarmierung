package de.msjones.android.alarmapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class ServerSettings(
    val host: String = "",
    val port: Int = 5672,
    val username: String = "",
    val password: String = "",
    val queue: String = "mobile_notifications"
)

object SettingsKeys {
    val HOST = stringPreferencesKey("host")
    val PORT = intPreferencesKey("port")
    val USER = stringPreferencesKey("user")
    val PASS = stringPreferencesKey("pass")
    val QUEUE = stringPreferencesKey("queue")
}

class SettingsStore(private val context: Context) {

    val flow: Flow<ServerSettings> = context.dataStore.data.map { prefs: Preferences ->
        ServerSettings(
            host = prefs[SettingsKeys.HOST] ?: "",
            port = prefs[SettingsKeys.PORT] ?: 5672,
            username = prefs[SettingsKeys.USER] ?: "",
            password = prefs[SettingsKeys.PASS] ?: "",
            queue = prefs[SettingsKeys.QUEUE] ?: "mobile_notifications"
        )
    }

    suspend fun save(s: ServerSettings) {
        context.dataStore.edit { e ->
            e[SettingsKeys.HOST] = s.host
            e[SettingsKeys.PORT] = s.port
            e[SettingsKeys.USER] = s.username
            e[SettingsKeys.PASS] = s.password
            e[SettingsKeys.QUEUE] = s.queue
        }
    }
}
