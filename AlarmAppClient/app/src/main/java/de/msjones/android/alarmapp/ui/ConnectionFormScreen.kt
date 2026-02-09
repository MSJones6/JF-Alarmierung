package de.msjones.android.alarmapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.msjones.android.alarmapp.data.ServerSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionFormScreen(
    existingConnections: List<ServerSettings>,
    editingConnection: ServerSettings? = null,
    initialHost: String? = null,
    initialPort: String? = null,
    initialUser: String? = null,
    initialPass: String? = null,
    initialTopic: String? = null,
    onSave: (ServerSettings) -> Unit,
    onCancel: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var duplicateTopicMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = editingConnection != null

    var host by rememberSaveable { mutableStateOf(editingConnection?.host ?: initialHost ?: "") }
    var port by rememberSaveable { mutableStateOf(editingConnection?.port?.toString() ?: initialPort ?: "1883") }
    var user by rememberSaveable { mutableStateOf(editingConnection?.username ?: initialUser ?: "") }
    var pass by rememberSaveable { mutableStateOf(editingConnection?.password ?: initialPass ?: "") }
    var topic by rememberSaveable { mutableStateOf(editingConnection?.topic ?: initialTopic ?: "JF/Alarm") }

    LaunchedEffect(duplicateTopicMessage) {
        duplicateTopicMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            duplicateTopicMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Verbindung bearbeiten" else "Neue Verbindung") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { ch -> ch.isDigit() } },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Passwort") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Queue-Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val trimmedTopic = topic.trim().ifEmpty { "JF/Alarm" }

                        // Check for duplicate topic (excluding the current connection when editing)
                        val existingTopic = existingConnections.any {
                            it.topic.equals(trimmedTopic, ignoreCase = true) &&
                                    it.id != editingConnection?.id
                        }

                        if (existingTopic) {
                            duplicateTopicMessage = "Diese Queue-Name existiert bereits!"
                            return@Button
                        }

                        val settings = ServerSettings(
                            id = editingConnection?.id ?: java.util.UUID.randomUUID().toString(),
                            host = host.trim(),
                            port = port.toIntOrNull() ?: 1883,
                            username = user.trim(),
                            password = pass,
                            topic = trimmedTopic,
                            isActive = editingConnection?.isActive ?: false
                        )
                        onSave(settings)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isEditing) "Aktualisieren" else "Hinzufügen")
                }

                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Abbrechen")
                }
            }
        }
    }
}
