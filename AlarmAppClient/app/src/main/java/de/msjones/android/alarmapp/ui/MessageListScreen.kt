package de.msjones.android.alarmapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import de.msjones.android.alarmapp.data.AlarmMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    viewModel: MessageViewModel,
    onSettingsClick: () -> Unit
) {
    val messages = viewModel.messages.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("JF Alarm App") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
            }
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            itemsIndexed(messages.value, key = { index, _ -> messages.value[index].id }) { index, msg ->

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { newValue ->
                        if (newValue == SwipeToDismissBoxValue.StartToEnd ||
                            newValue == SwipeToDismissBoxValue.EndToStart
                        ) {
                            viewModel.removeMessage(index)
                        }
                        true
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Red)
                        )
                    },
                    content = {
                        MessageCard(message = msg, onDeleteClick = { viewModel.removeMessage(index) })
                    }
                )
            }
        }
    }
}

@Composable
fun MessageCard(message: AlarmMessage, onDeleteClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(message.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
