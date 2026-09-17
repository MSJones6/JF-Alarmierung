package de.msjones.android.alarmapp.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
 * Bildschirm zur Verwaltung von MQTT-Verbindungen und deren einzelnem Aktiv-Status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    connections: List<ServerSettings>,
    onSaveConnection: (ServerSettings) -> Unit,
    onDeleteConnection: (String) -> Unit,
    onToggleConnection: (ServerSettings, Boolean) -> Unit,
    onConnectionFailed: (String) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var runtimeStatuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var screenState by remember { mutableStateOf<SettingsScreenState>(SettingsScreenState.List) }
    var scannedConnectionFromQr by remember { mutableStateOf<ServerSettings?>(null) }

    LaunchedEffect(Unit) {
        MessagingEventBus.events.collect { event ->
            when (event) {
                is MessagingEvent.AuthError -> {
                    authErrorMessage = event.errorMessage
                    if (event.connectionId.isNotBlank()) {
                        runtimeStatuses = runtimeStatuses + (event.connectionId to "ERROR")
                        onConnectionFailed(event.connectionId)
                    }
                }
                is MessagingEvent.StopAllConnections -> {
                    runtimeStatuses = emptyMap()
                    screenState = SettingsScreenState.List
                }
                is MessagingEvent.ServiceRunningState -> {
                    if (!event.isRunning) {
                        runtimeStatuses = emptyMap()
                    }
                }
                is MessagingEvent.ConnectionState -> {
                    if (event.connectionId.isNotBlank()) {
                        runtimeStatuses = runtimeStatuses + (event.connectionId to event.status)
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
                runtimeStatuses = runtimeStatuses,
                snackbarHostState = snackbarHostState,
                onAddConnection = { screenState = SettingsScreenState.Add },
                onScanQr = { screenState = SettingsScreenState.ScanQr },
                onEditConnection = { screenState = SettingsScreenState.Edit(it) },
                onDeleteConnection = onDeleteConnection,
                onToggleConnection = onToggleConnection
            )
        }
    }
}

/**
 * Listenansicht der gespeicherten Verbindungen mit einzelnem Aktiv-Schalter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsListContent(
    connections: List<ServerSettings>,
    runtimeStatuses: Map<String, String>,
    snackbarHostState: SnackbarHostState,
    onAddConnection: () -> Unit,
    onScanQr: () -> Unit,
    onEditConnection: (ServerSettings) -> Unit,
    onDeleteConnection: (String) -> Unit,
    onToggleConnection: (ServerSettings, Boolean) -> Unit
) {
    val enabledCount = connections.count { it.isActive }

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
                    Text("Gespeicherte Verbindungen", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (enabledCount > 0) {
                            "$enabledCount von ${connections.size} Verbindungen aktiv"
                        } else {
                            "Keine Verbindung aktiv"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabledCount > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
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
                        runtimeStatus = runtimeStatuses[connection.id],
                        onEdit = { onEditConnection(connection) },
                        onDelete = { onDeleteConnection(connection.id) },
                        onToggleEnabled = { enabled -> onToggleConnection(connection, enabled) }
                    )
                }
            }
        }
    }
}

/**
 * Karte für eine einzelne MQTT-Verbindung inklusive Aktiv-Schalter.
 *
 * @param connection gespeicherte Verbindung
 * @param runtimeStatus aktueller MQTT-Status oder null
 * @param onEdit öffnet das Bearbeitungsformular
 * @param onDelete löscht die Verbindung
 * @param onToggleEnabled aktiviert oder deaktiviert genau diese Verbindung
 */
@Composable
private fun ConnectionCard(
    connection: ServerSettings,
    runtimeStatus: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
) {
    val indicatorColor = connectionStatusColor(connection.isActive, runtimeStatus)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (connection.isActive) {
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
                tint = if (connection.isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
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
                    .background(color = indicatorColor, shape = CircleShape)
            )

            Spacer(Modifier.width(8.dp))

            Switch(
                checked = connection.isActive,
                onCheckedChange = onToggleEnabled
            )

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

/**
 * Wählt die Indikatorfarbe anhand Aktiv-Status und Laufzeitstatus.
 *
 * @param isEnabled ob die Verbindung vom Nutzer aktiviert ist
 * @param runtimeStatus letzter MQTT-Status oder null
 * @return Farbe für den Statuspunkt
 */
@Composable
private fun connectionStatusColor(isEnabled: Boolean, runtimeStatus: String?): Color {
    val status = runtimeStatus.orEmpty().uppercase()
    return when {
        !isEnabled -> Color.Gray
        status == "ERROR" -> MaterialTheme.colorScheme.error
        status == "SUBSCRIBED" || status == "CONNECTED" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
}
