package de.msjones.android.alarmapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.msjones.android.alarmapp.data.ServerSettings
import de.msjones.android.alarmapp.data.ServerSettings.Companion.fromQrCode
import de.msjones.android.alarmapp.event.MessagingEvent
import de.msjones.android.alarmapp.event.MessagingEventBus

/**
 * Navigationszustände innerhalb des Einstellungsbildschirms.
 */
sealed class SettingsScreenState {
    data object List : SettingsScreenState()
    data class Edit(val connection: ServerSettings) : SettingsScreenState()
    data object Add : SettingsScreenState()
    data object ScanQr : SettingsScreenState()
}

/**
 * Bildschirm zur Verwaltung von MQTT-Verbindungen und des Messaging-Dienstes.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val snackbarHostState = remember { SnackbarHostState() }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var serviceEnabled by remember(isServiceRunning) { mutableStateOf(isServiceRunning) }
    var activeConnectionCount by remember { mutableIntStateOf(0) }
    var screenState by remember { mutableStateOf<SettingsScreenState>(SettingsScreenState.List) }
    var scannedConnectionFromQr by remember { mutableStateOf<ServerSettings?>(null) }

    LaunchedEffect(Unit) {
        MessagingEventBus.events.collect { event ->
            when (event) {
                is MessagingEvent.AuthError -> {
                    authErrorMessage = event.errorMessage
                }
                is MessagingEvent.StopAllConnections -> {
                    serviceEnabled = false
                    screenState = SettingsScreenState.List
                }
                is MessagingEvent.ServiceRunningState -> {
                    serviceEnabled = event.isRunning
                    if (!event.isRunning && activeConnectionCount > 0) {
                        activeConnectionCount = 0
                    }
                }
                is MessagingEvent.ConnectionState -> {
                    when (event.status.uppercase()) {
                        "SUBSCRIBED" -> activeConnectionCount += 1
                        "DISCONNECTED", "ERROR" -> {
                            if (activeConnectionCount > 0) {
                                activeConnectionCount -= 1
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(authErrorMessage) {
        authErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            authErrorMessage = null
            serviceEnabled = false
            activeConnectionCount = 0
            onServiceFailed()
        }
    }

    when (val state = screenState) {
        is SettingsScreenState.Add -> {
            ConnectionFormScreen(
                existingConnections = connections,
                editingConnection = null,
                initialHost = scannedConnectionFromQr?.host,
                initialPort = scannedConnectionFromQr?.port?.toString(),
                initialUser = scannedConnectionFromQr?.username,
                initialPass = scannedConnectionFromQr?.password,
                initialTopic = scannedConnectionFromQr?.topic,
                initialSsl = scannedConnectionFromQr?.ssl,
                onSave = { settings ->
                    onSaveConnection(settings)
                    scannedConnectionFromQr = null
                    screenState = SettingsScreenState.List
                },
                onCancel = {
                    scannedConnectionFromQr = null
                    screenState = SettingsScreenState.List
                }
            )
        }

        is SettingsScreenState.Edit -> {
            ConnectionFormScreen(
                existingConnections = connections,
                editingConnection = state.connection,
                onSave = { settings ->
                    onSaveConnection(settings)
                    screenState = SettingsScreenState.List
                },
                onCancel = { screenState = SettingsScreenState.List }
            )
        }

        is SettingsScreenState.ScanQr -> {
            QrCodeScannerScreen(
                onQrCodeScanned = { qrContent ->
                    val settings = fromQrCode(qrContent)
                    if (settings != null) {
                        scannedConnectionFromQr = settings
                        screenState = SettingsScreenState.Add
                    } else {
                        screenState = SettingsScreenState.List
                    }
                },
                onCancel = { screenState = SettingsScreenState.List },
                onError = {
                    screenState = SettingsScreenState.List
                }
            )
        }

        SettingsScreenState.List -> {
            SettingsListContent(
                connections = connections,
                activeConnectionId = activeConnectionId,
                serviceEnabled = serviceEnabled,
                activeConnectionCount = activeConnectionCount,
                snackbarHostState = snackbarHostState,
                onStartAllServices = onStartAllServices,
                onStopAllServices = onStopAllServices,
                onAddConnection = { screenState = SettingsScreenState.Add },
                onScanQr = { screenState = SettingsScreenState.ScanQr },
                onEditConnection = { screenState = SettingsScreenState.Edit(it) },
                onDeleteConnection = onDeleteConnection,
                onSetActiveConnection = onSetActiveConnection
            )
        }
    }
}

/**
 * Listenansicht der gespeicherten Verbindungen inklusive Dienst-Schalter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsListContent(
    connections: List<ServerSettings>,
    activeConnectionId: String?,
    serviceEnabled: Boolean,
    activeConnectionCount: Int,
    snackbarHostState: SnackbarHostState,
    onStartAllServices: () -> Unit,
    onStopAllServices: () -> Unit,
    onAddConnection: () -> Unit,
    onScanQr: () -> Unit,
    onEditConnection: (ServerSettings) -> Unit,
    onDeleteConnection: (String) -> Unit,
    onSetActiveConnection: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Einstellungen") }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(action = {
                    TextButton(
                        onClick = { snackbarHostState.currentSnackbarData?.dismiss() }
                    ) { Text("OK") }
                }) { Text(data.visuals.message) }
            }
        }
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
                Column {
                    Text("Dienst aktiv", style = MaterialTheme.typography.bodyLarge)
                    if (activeConnectionCount > 0) {
                        Text(
                            text = "$activeConnectionCount Verbindungen aktiv",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                var switchChecked by remember { mutableStateOf(serviceEnabled) }

                LaunchedEffect(serviceEnabled) {
                    switchChecked = serviceEnabled
                }

                Switch(
                    checked = switchChecked,
                    onCheckedChange = { enabled ->
                        switchChecked = enabled
                        if (enabled) {
                            onStartAllServices()
                        } else {
                            onStopAllServices()
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gespeicherte Verbindungen", style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onScanQr) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR-Code scannen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onAddConnection) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Neue Verbindung hinzufügen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxHeight(1f)
            ) {
                items(connections) { connection ->
                    ConnectionCard(
                        connection = connection,
                        isActive = connection.id == activeConnectionId,
                        onEdit = { onEditConnection(connection) },
                        onDelete = { onDeleteConnection(connection.id) },
                        onActivate = { onSetActiveConnection(connection.id) }
                    )
                }
            }
        }
    }
}

/**
 * Karte für eine einzelne MQTT-Verbindung.
 */
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

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Bearbeiten",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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
