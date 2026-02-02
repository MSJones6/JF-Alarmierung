package de.msjones.android.alarmapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.msjones.android.alarmapp.data.AlarmMessage
import de.msjones.android.alarmapp.data.MessageStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MessageStore(application)

    val messages: StateFlow<List<AlarmMessage>> = store.flow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addMessage(msg: String) {
        viewModelScope.launch {
            store.addMessage(msg)
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
