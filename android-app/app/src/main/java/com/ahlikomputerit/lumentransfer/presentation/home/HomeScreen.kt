package com.ahlikomputerit.lumentransfer.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onSend: () -> Unit,
    onReceive: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.LightMode,
            contentDescription = "Lumen Transfer",
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Lumen Transfer", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pindahkan file melalui cahaya: layar pengirim menampilkan QR, kamera penerima membacanya.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "Privasi: transfer offline tanpa server, Wi-Fi, Bluetooth, atau kabel. Isi file belum dienkripsi."
            },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Offline by design", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Tidak ada server, pairing, Wi-Fi, Bluetooth, atau kabel dalam jalur transfer. " +
                        "QR dapat terlihat oleh kamera lain di sekitar dan MVP ini belum mengenkripsi isi file.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSend,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Kirim file. Membuka privacy notice jika ini transfer pertama." },
        ) {
            Icon(Icons.Outlined.UploadFile, contentDescription = null)
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text("Kirim file")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onReceive,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Terima file. Membuka privacy notice jika ini transfer pertama." },
        ) {
            Icon(Icons.Outlined.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text("Terima file")
        }
    }
}
