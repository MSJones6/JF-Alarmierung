package de.msjones.android.alarmapp.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import de.msjones.android.alarmapp.data.ServerSettings

@Composable
fun SettingsScreen(
    connections: List<ServerSettings>,
    activeConnectionId: String?,
    onSaveConnection: (ServerSettings) -> Unit,
    onDeleteConnection: (String) -> Unit,
    onSetActiveConnection: (String) -> Unit,
    onStartAllServices: () -> Unit,
    onStopAllServices: () -> Unit,
    onServiceFailed: () -> Unit = {},
    isServiceRunning: Boolean = false,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var duplicateTopicMessage by remember { mutableStateOf<String?>(null) }
    var serviceEnabled by remember(isServiceRunning) { mutableStateOf(isServiceRunning) }

    // Edit mode state
    var editingConnection by remember { mutableStateOf<ServerSettings?>(null) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("1883") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("JF/Alarm") }

    // Register broadcast receiver for auth errors
    val authErrorReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "AUTH_ERROR") {
                    val errorMessage = intent.getStringExtra("error_message") ?: "Authentifizierungsfehler"
                    authErrorMessage = errorMessage
                }
            }
        }
    }

    // Register broadcast receiver for stop all connections (e.g., due to connection error)
    val stopAllReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "STOP_ALL_CONNECTIONS") {
                    serviceEnabled = false
                }
            }
        }
    }

    LaunchedEffect(authErrorMessage) {
        authErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            authErrorMessage = null
            serviceEnabled = false
            onServiceFailed()
        }
    }

    LaunchedEffect(duplicateTopicMessage) {
        duplicateTopicMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            duplicateTopicMessage = null
        }
    }

    LaunchedEffect(Unit) {
        val authFilter = IntentFilter("AUTH_ERROR")
        LocalBroadcastManager.getInstance(context).registerReceiver(authErrorReceiver, authFilter)
        
        val stopAllFilter = IntentFilter("STOP_ALL_CONNECTIONS")
        LocalBroadcastManager.getInstance(context).registerReceiver(stopAllReceiver, stopAllFilter)
    }

    DisposableEffect(Unit) {
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(authErrorReceiver)
            LocalBroadcastManager.getInstance(context).unregisterReceiver(stopAllReceiver)
        }
    }

    // Load connection into form when editing
    LaunchedEffect(editingConnection) {
        editingConnection?.let { conn ->
            host = conn.host
            port = conn.port.toString()
            user = conn.username
            pass = conn.password
            topic = conn.topic
        }
    }

    fun resetForm() {
        editingConnection = null
        host = ""
        port = "1883"
        user = ""
        pass = ""
        topic = "JF/Alarm"
    }

    fun loadConnectionForEdit(connection: ServerSettings) {
        editingConnection = connection
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)) {
            Text("Server Einstellungen", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(host, { host = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(port, { port = it.filter { ch -> ch.isDigit() } }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(user, { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(pass, { pass = it }, label = { Text("Passwort") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(topic, { topic = it }, label = { Text("Queue-Name") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val trimmedTopic = topic.trim().ifEmpty { "JF/Alarm" }
                    
                    // Check for duplicate topic (excluding the current connection when editing)
                    val existingTopic = connections.any { 
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
                        isActive = activeConnectionId == editingConnection?.id || (editingConnection == null && connections.isEmpty())
                    )
                    onSaveConnection(settings)
                    resetForm()
                }) { Text(if (editingConnection != null) "Aktualisieren" else "Hinzufügen") }

                if (editingConnection != null) {
                    TextButton(onClick = { resetForm() }) {
                        Text("Abbrechen")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            // Global service control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dienst aktiv", style = MaterialTheme.typography.bodyLarge)
                    if (isServiceRunning) {
                        Text(
                            text = "${connections.size} Verbindungen aktiv",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Switch(
                    checked = serviceEnabled,
                    onCheckedChange = { enabled ->
                        serviceEnabled = enabled
                        if (enabled) {
                            onStartAllServices()
                        } else {
                            onStopAllServices()
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Gespeicherte Verbindungen", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(connections) { connection ->
                    ConnectionCard(
                        connection = connection,
                        isActive = connection.id == activeConnectionId,
                        onEdit = { loadConnectionForEdit(connection) },
                        onDelete = { onDeleteConnection(connection.id) },
                        onActivate = { onSetActiveConnection(connection.id) }
                    )
                }
            }

            // Show auth error snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            ) { data ->
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarHostState.currentSnackbarData?.dismiss() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(data.visuals.message)
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    connection: ServerSettings,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onActivate: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onActivate() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.host.ifEmpty { "Unbenannt" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${connection.host}:${connection.port}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (connection.topic.isNotEmpty()) {
                    Text(
                        text = "Topic: ${connection.topic}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Active indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            Spacer(Modifier.width(8.dp))

            // Edit button
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Bearbeiten",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
