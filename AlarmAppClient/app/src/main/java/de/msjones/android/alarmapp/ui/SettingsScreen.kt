package de.msjones.android.alarmapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.msjones.android.alarmapp.data.ServerSettings

@Composable
fun SettingsScreen(
    initial: ServerSettings,
    onSave: (ServerSettings) -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
) {
    var host by remember { mutableStateOf(initial.host) }
    var port by remember { mutableStateOf(initial.port.toString()) }
    var user by remember { mutableStateOf(initial.username) }
    var pass by remember { mutableStateOf(initial.password) }
    var queue by remember { mutableStateOf(initial.queue) }

    Column(Modifier
        .fillMaxSize()
        .padding(WindowInsets.systemBars.asPaddingValues())
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
        OutlinedTextField(queue, { queue = it }, label = { Text("Queue-Name") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onSave(
                    ServerSettings(
                        host = host.trim(),
                        port = port.toIntOrNull() ?: 5672,
                        username = user.trim(),
                        password = pass,
                        queue = queue.trim().ifEmpty { "mobile_notifications" }
                    )
                )
            }) { Text("Speichern") }

            Button(onClick = onStartService) { Text("Service starten") }
            OutlinedButton(onClick = onStopService) { Text("Service stoppen") }
        }
    }
}
