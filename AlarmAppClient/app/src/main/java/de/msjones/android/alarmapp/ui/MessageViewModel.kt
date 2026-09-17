package de.msjones.android.alarmapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.msjones.android.alarmapp.data.AlarmMessage
import de.msjones.android.alarmapp.data.MessageStore
import de.msjones.android.alarmapp.data.ServerSettings
import de.msjones.android.alarmapp.data.SettingsStore
import de.msjones.android.alarmapp.event.MessagingEvent
import de.msjones.android.alarmapp.event.MessagingEventBus
import de.msjones.android.alarmapp.util.ConnectionStatusTexts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /**
     * Verdichteter Status aller Verbindungen, unabhängig vom zuletzt empfangenen Einzelereignis.
     */
    val connectionStatus: StateFlow<ConnectionStatus> = combine(
        settingsStore.flow,
        settingsStore.runtimeStatuses,
        settingsStore.runtimeMessages
    ) { connections, statuses, messages ->
        toConnectionStatus(connections, statuses, messages)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = toConnectionStatus(
            settingsStore.getConnectionsSnapshot(),
            settingsStore.runtimeStatuses.value,
            settingsStore.runtimeMessages.value
        )
    )

    private val _userError = MutableStateFlow<String?>(null)

    /** Kurz anzuzeigende Fehlermeldung, ohne den Gesamtstatus zu löschen. */
    val userError: StateFlow<String?> = _userError.asStateFlow()

    init {
        viewModelScope.launch {
            MessagingEventBus.events.collect { event ->
                when (event) {
                    is MessagingEvent.NewMessage -> {
                        if (event.keyword.isNotBlank() ||
                            event.location.isNotBlank() ||
                            event.extras.isNotBlank()
                        ) {
                            store.addMessage(event.keyword, event.location, event.extras)
                        }
                    }
                    is MessagingEvent.AuthError -> {
                        if (event.errorMessage.isNotBlank()) {
                            _userError.value = event.errorMessage
                        }
                    }
                    is MessagingEvent.ConnectionState -> {
                        if (event.status.uppercase() == "ERROR" && event.message.isNotBlank()) {
                            _userError.value = event.message
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * Baut den Anzeigestatus aus allen Verbindungen und deren Laufzeitphasen.
     *
     * @param connections gespeicherte Verbindungen
     * @param statuses Statuscode je Verbindungs-ID
     * @param messages Anzeigetext je Verbindungs-ID
     * @return verdichteter Status für die Nachrichtenliste
     */
    private fun toConnectionStatus(
        connections: List<ServerSettings>,
        statuses: Map<String, String>,
        messages: Map<String, String>
    ): ConnectionStatus {
        val phases = ConnectionStatusTexts.phasesFor(connections, statuses)
        return ConnectionStatus(
            status = ConnectionStatusTexts.statusCode(
                ConnectionStatusTexts.overallPhase(phases)
            ),
            message = ConnectionStatusTexts.displaySummary(connections, statuses, messages),
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Löscht eine bereits angezeigte Fehlermeldung.
     */
    fun clearUserError() {
        _userError.value = null
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
