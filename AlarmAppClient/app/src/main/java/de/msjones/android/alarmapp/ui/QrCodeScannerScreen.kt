package de.msjones.android.alarmapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Startet den Play-Services-Code-Scanner für Verbindungs-QR-Codes.
 *
 * Im Gegensatz zu CameraX/ML-Kit-Bundled enthält diese Variante keine nativen
 * Bibliotheken in der App-APK und vermeidet damit 16-KB-RELRO-Probleme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScannerScreen(
    onQrCodeScanned: (String) -> Unit,
    onCancel: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (started) return@LaunchedEffect
        started = true

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val scanner = GmsBarcodeScanning.getClient(context, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue
                if (value.isNullOrBlank()) {
                    onError("QR-Code konnte nicht gelesen werden.")
                } else {
                    onQrCodeScanned(value)
                }
            }
            .addOnCanceledListener {
                onCancel()
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "QR-Scan fehlgeschlagen.")
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR-Code scannen") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Abbrechen"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
