package de.msjones.android.alarmapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.msjones.android.alarmapp.data.AlarmMessage
import de.msjones.android.alarmapp.data.MessageStore
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.event.MessagingEvent
import de.msjones.android.alarmapp.event.MessagingEventBus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Verbindungsstatus für die Anzeige in der Nachrichtenliste.
 */
data class ConnectionStatus(
    val status: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

/**
 * ViewModel für Alarmnachrichten und Verbindungsstatus.
 */
class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MessageStore(application)
    private val settingsStore = SettingsStore.getInstance(application)

    val messages: StateFlow<List<AlarmMessage>> = store.flow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    init {
        viewModelScope.launch {
            MessagingEventBus.events.collect { event ->
                if (event is MessagingEvent.NewMessage &&
                    (event.keyword.isNotBlank() ||
                        event.location.isNotBlank() ||
                        event.extras.isNotBlank())
                ) {
                    store.addMessage(event.keyword, event.location, event.extras)
                }
            }
        }
    }

    /**
     * Löscht den gespeicherten Verbindungsstatus (z. B. nach Anzeige eines Fehlers).
     */
    fun clearConnectionStatus() {
        viewModelScope.launch {
            settingsStore.clearConnectionStatus()
        }
    }

    /**
     * Fügt eine Alarmnachricht manuell hinzu.
     */
    fun addMessage(keyword: String, location: String, extras: String) {
        viewModelScope.launch {
            store.addMessage(keyword, location, extras)
        }
    }

    /**
     * Entfernt eine Nachricht anhand ihres Listenindex.
     *
     * @param index Position in der aktuellen Nachrichtenliste
     */
    fun removeMessage(index: Int) {
        viewModelScope.launch {
            val currentList = messages.value
            if (index in currentList.indices) {
                store.removeMessage(currentList[index].id)
            }
        }
    }

    /**
     * Löscht alle gespeicherten Alarmnachrichten.
     */
    fun clearAllMessages() {
        viewModelScope.launch {
            store.clearAllMessages()
        }
    }
}
