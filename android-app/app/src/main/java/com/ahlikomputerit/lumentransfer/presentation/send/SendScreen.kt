package com.ahlikomputerit.lumentransfer.presentation.send

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    viewModel: SendViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::onFileSelected)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kirim file") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("‹") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pilih file. QR animasi dan pengiriman optik akan diaktifkan pada checkpoint berikutnya.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Pilih file untuk dikirim" },
            ) {
                Text("Pilih file")
            }

            when (val status = state.status) {
                SendStatus.Idle -> InfoCard("Belum ada file", "File tidak pernah dikirim ke server.")
                SendStatus.Reading -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Membaca metadata dan menghitung SHA-256…")
                    }
                }
                SendStatus.Ready -> state.selected?.let { selected ->
                    InfoCard(
                        title = selected.metadata.fileName,
                        body = "${selected.metadata.mimeType} · ${selected.metadata.sizeBytes} bytes\nSHA-256: ${selected.metadata.sha256.toHex()}",
                    )
                }
                is SendStatus.Failed -> InfoCard("Tidak dapat membaca file", status.message)
            }

            if (state.selected != null) {
                Button(onClick = viewModel::clearSelection, modifier = Modifier.fillMaxWidth()) {
                    Text("Hapus pilihan")
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
