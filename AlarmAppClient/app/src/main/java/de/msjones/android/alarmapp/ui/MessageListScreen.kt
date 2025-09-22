package de.msjones.android.alarmapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.material.DismissDirection
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
            itemsIndexed(messages.value, key = { index, _ -> index }) { index, msg ->

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { newValue ->
                        // newValue ist vom Typ SwipeToDismissBoxValue
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                msg,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}
