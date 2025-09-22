package de.msjones.android.alarmapp.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MessageViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

    fun addMessage(msg: String) {
        _messages.value = _messages.value + msg
    }

    fun removeMessage(index: Int) {
        _messages.value = _messages.value.toMutableList().apply { removeAt(index) }
    }
}