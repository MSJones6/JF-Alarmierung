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
import androidx.compose.material3.Switch
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.msjones.android.alarmapp.data.ConnectionActivation
import de.msjones.android.alarmapp.data.ServerSettings

/**
 * Formular zum Anlegen oder Bearbeiten einer MQTT-Verbindung.
 *
 * Tab und Enter setzen den Fokus auf das nächste Eingabefeld, Umschalt+Tab auf das vorherige.
 */
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
    initialSsl: Boolean? = null,
    onSave: (ServerSettings) -> Unit,
    onCancel: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var duplicateConnectionMessage by remember { mutableStateOf<String?>(null) }

    val isEditing = editingConnection != null

    var ssl by rememberSaveable { mutableStateOf(editingConnection?.ssl ?: initialSsl ?: false) }
    var host by rememberSaveable { mutableStateOf(editingConnection?.host ?: initialHost ?: "") }
    var port by rememberSaveable { mutableStateOf(editingConnection?.port?.toString() ?: initialPort ?: "1883") }
    var user by rememberSaveable { mutableStateOf(editingConnection?.username ?: initialUser ?: "") }
    var pass by rememberSaveable { mutableStateOf(editingConnection?.password ?: initialPass ?: "") }
    var topic by rememberSaveable { mutableStateOf(editingConnection?.topic ?: initialTopic ?: "JF/Alarm/KB") }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(duplicateConnectionMessage) {
        duplicateConnectionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            duplicateConnectionMessage = null
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SSL")
                Switch(
                    checked = ssl,
                    onCheckedChange = { ssl = it }
                )
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .moveFocusOnTabOrEnter(focusManager)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { ch -> ch.isDigit() } },
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .moveFocusOnTabOrEnter(focusManager)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .moveFocusOnTabOrEnter(focusManager)
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Passwort") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .moveFocusOnTabOrEnter(focusManager),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Queue-Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .moveFocusOnTabOrEnter(focusManager, isLastField = true)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val trimmedHost = host.trim()
                        val trimmedTopic = topic.trim().ifEmpty { "JF/Alarm/KB" }
                        val parsedPort = port.toIntOrNull() ?: 1883

                        val isDuplicate = ConnectionActivation.isDuplicateConnection(
                            connections = existingConnections,
                            host = trimmedHost,
                            port = parsedPort,
                            topic = trimmedTopic,
                            excludeId = editingConnection?.id
                        )

                        if (isDuplicate) {
                            duplicateConnectionMessage =
                                "Diese Verbindung mit Host, Port und Queue existiert bereits!"
                            return@Button
                        }

                        val settings = ServerSettings(
                            id = editingConnection?.id ?: java.util.UUID.randomUUID().toString(),
                            host = trimmedHost,
                            port = parsedPort,
                            username = user.trim(),
                            password = pass,
                            topic = trimmedTopic,
                            isActive = editingConnection?.isActive ?: false,
                            ssl = ssl
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
