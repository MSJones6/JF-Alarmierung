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
    val port: Int = 1883,
    val username: String = "",
    val password: String = "",
    val topic: String = "JF/Alarm"
)

object SettingsKeys {
    val HOST = stringPreferencesKey("host")
    val PORT = intPreferencesKey("port")
    val USER = stringPreferencesKey("user")
    val PASS = stringPreferencesKey("pass")
    val TOPIC = stringPreferencesKey("topic")
}

class SettingsStore(private val context: Context) {

    val flow: Flow<ServerSettings> = context.dataStore.data.map { prefs: Preferences ->
        ServerSettings(
            host = prefs[SettingsKeys.HOST] ?: "",
            port = prefs[SettingsKeys.PORT] ?: 1883,
            username = prefs[SettingsKeys.USER] ?: "",
            password = prefs[SettingsKeys.PASS] ?: "",
            topic = prefs[SettingsKeys.TOPIC] ?: ""
        )
    }

    suspend fun save(s: ServerSettings) {
        context.dataStore.edit { e ->
            e[SettingsKeys.HOST] = s.host
            e[SettingsKeys.PORT] = s.port
            e[SettingsKeys.USER] = s.username
            e[SettingsKeys.PASS] = s.password
            e[SettingsKeys.TOPIC] = s.topic
        }
    }
}
