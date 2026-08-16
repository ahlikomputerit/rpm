package com.ahlikomputerit.lumentransfer.presentation.send

import android.content.Intent
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ahlikomputerit.lumentransfer.data.file.DiagnosticsFileWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    viewModel: SendViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.pauseSending()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.cancelSending()
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::onFileSelected)
    }
    val diagnosticsExporter = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            runCatching { DiagnosticsFileWriter.write(context.contentResolver, uri, diagnostics) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kirim file") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Kembali ke beranda" },
                    ) { Text("‹") }
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
                "Pilih file. Naikkan kecerahan layar dan letakkan perangkat sekitar 15–30 cm dari kamera penerima.",
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    ) {
                        CircularProgressIndicator()
                        Text("Membaca metadata dan menghitung SHA-256…")
                    }
                }
                SendStatus.Ready -> {
                    state.selected?.let { selected ->
                        InfoCard(
                            title = selected.metadata.fileName,
                            body = "${selected.metadata.mimeType} · ${selected.metadata.sizeBytes} bytes\nSHA-256: ${selected.metadata.sha256.toHex()}",
                        )
                    }
                    InfoCard(
                        title = "Panduan layar pengirim",
                        body = "Gunakan kecerahan layar tinggi, matikan auto-lock sementara, dan jaga jarak sekitar 15–30 cm. Pastikan QR terlihat penuh pada kamera penerima.",
                    )
                    state.qrPreview?.let { preview ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Preview frame QR", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                QrMatrixCanvas(
                                    matrix = preview,
                                    contentDescription = "Frame QR transfer optik. Arahkan kamera penerima ke seluruh kode QR.",
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Preview ini menunjukkan satu frame protocol. Saat loop dimulai, frame akan berubah otomatis.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                is SendStatus.Failed -> InfoCard("Tidak dapat membaca file", status.message, isError = true)
            }

            if (state.selected != null) {
                when (val sender = state.senderStatus) {
                    SenderStatus.Idle -> Button(
                        onClick = viewModel::startSending,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Mulai menampilkan loop QR" },
                    ) { Text("Mulai loop QR") }
                    is SenderStatus.Running -> {
                        Text(
                            "Frame ${sender.frameNumber} · ${sender.kind}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = viewModel::pauseSending,
                                modifier = Modifier.weight(1f).semantics { contentDescription = "Jedaikan pengiriman QR" },
                            ) { Text("Pause") }
                            OutlinedButton(
                                onClick = viewModel::cancelSending,
                                modifier = Modifier.weight(1f).semantics { contentDescription = "Hentikan pengiriman QR" },
                            ) { Text("Stop") }
                        }
                    }
                    is SenderStatus.Paused -> {
                        Text(
                            "Paused pada frame ${sender.frameNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = viewModel::startSending,
                                modifier = Modifier.weight(1f).semantics { contentDescription = "Lanjutkan pengiriman QR" },
                            ) { Text("Lanjutkan") }
                            OutlinedButton(
                                onClick = viewModel::cancelSending,
                                modifier = Modifier.weight(1f).semantics { contentDescription = "Hentikan pengiriman QR" },
                            ) { Text("Stop") }
                        }
                    }
                }
                OutlinedButton(
                    onClick = viewModel::clearSelection,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Hapus file yang dipilih dan reset sesi" },
                ) { Text("Hapus pilihan") }
            }

            DiagnosticsSummaryCard(
                elapsedMs = diagnostics.elapsedMs,
                primary = "Frame emitted: ${diagnostics.emittedFrames}",
                secondary = "Systematic/repair: ${diagnostics.systematicFrames}/${diagnostics.repairFrames}",
                tertiary = "Bytes emitted: ${diagnostics.emittedBytes}",
            )
            OutlinedButton(
                onClick = {
                    diagnosticsExporter.launch(
                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_TITLE, "lumen-diagnostics-send.json")
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Export diagnostics pengiriman sebagai JSON" },
            ) { Text("Export diagnostics") }
        }
    }
}

@Composable
private fun DiagnosticsSummaryCard(
    elapsedMs: Long,
    primary: String,
    secondary: String,
    tertiary: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Elapsed: ${elapsedMs} ms", style = MaterialTheme.typography.bodyMedium)
            Text(primary, style = MaterialTheme.typography.bodyMedium)
            Text(secondary, style = MaterialTheme.typography.bodyMedium)
            Text(tertiary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, isError: Boolean = false) {
    val accessibilityModifier = if (isError) {
        Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "Error: $title. $body"
        }
    } else {
        Modifier
    }
    Card(modifier = Modifier.fillMaxWidth().then(accessibilityModifier)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
