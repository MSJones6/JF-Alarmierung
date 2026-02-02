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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var topic by remember { mutableStateOf(initial.topic) }

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
                    onSave(
                        ServerSettings(
                            host = host.trim(),
                            port = port.toIntOrNull() ?: 1883,
                            username = user.trim(),
                            password = pass,
                            topic = topic.trim().ifEmpty { "JF/Alarm" }
                        )
                    )
                }) { Text("Speichern") }

                Button(onClick = onStartService) { Text("Service starten") }
                OutlinedButton(onClick = onStopService) { Text("Service stoppen") }
            }
        }
    }
}
