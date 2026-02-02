package de.msjones.android.alarmapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.msjones.android.alarmapp.data.AlarmMessage
import de.msjones.android.alarmapp.data.MessageStore
import de.msjones.android.alarmapp.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConnectionStatus(
    val status: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MessageStore(application)
    private val settingsStore = SettingsStore(application)

    val messages: StateFlow<List<AlarmMessage>> = store.flow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combine connection status fields into a single state
    val connectionStatus: StateFlow<ConnectionStatus> = combine(
        settingsStore.connectionStatus,
        settingsStore.connectionStatusMessage,
        settingsStore.connectionStatusTimestamp
    ) { status, message, timestamp ->
        ConnectionStatus(
            status = status ?: "",
            message = message ?: "",
            timestamp = timestamp ?: 0L
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectionStatus()
    )

    fun clearConnectionStatus() {
        viewModelScope.launch {
            settingsStore.clearConnectionStatus()
        }
    }

    fun addMessage(keyword: String, location: String, extras: String) {
        viewModelScope.launch {
            store.addMessage(keyword, location, extras)
        }
    }

    fun removeMessage(index: Int) {
        viewModelScope.launch {
            val currentList = messages.value
            if (index in currentList.indices) {
                store.removeMessage(currentList[index].id)
            }
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            store.clearAllMessages()
        }
    }
}
