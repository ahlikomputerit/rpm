package com.ahlikomputerit.lumentransfer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PrivacyNoticeDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sebelum transfer") },
        text = {
            Text(
                "Lumen Transfer mengirim file secara offline melalui animasi QR di layar dan kamera perangkat lain. " +
                    "Tidak ada server, Wi-Fi, Bluetooth, atau akun yang digunakan untuk transfer. " +
                    "QR yang tampil dapat terlihat oleh kamera lain di sekitar, dan MVP ini belum mengenkripsi isi file. " +
                    "SHA-256 hanya memeriksa integritas file; diagnostics hanya menyimpan metrik tanpa payload.",
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("Saya mengerti")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}
