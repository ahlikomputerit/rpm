package com.ahlikomputerit.lumentransfer.presentation.receive

import android.Manifest
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ahlikomputerit.lumentransfer.data.camera.CameraFrameAnalyzer
import com.ahlikomputerit.lumentransfer.data.camera.CameraXSession
import com.ahlikomputerit.lumentransfer.data.file.DiagnosticsFileWriter
import com.ahlikomputerit.lumentransfer.data.qr.ZxingQrImageDecoder
import com.ahlikomputerit.lumentransfer.domain.diagnostics.TransferDiagnostics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val previewView = remember { PreviewView(context) }
    val cameraSession = remember { CameraXSession(context) }
    val qrImageDecoder = remember { ZxingQrImageDecoder() }
    val analyzer = remember(viewModel, qrImageDecoder) {
        CameraFrameAnalyzer(
            qrDecoder = qrImageDecoder,
            onEnvelope = viewModel::onEnvelope,
            onRejected = viewModel::onRejected,
        )
    }

    val diagnosticsExporter = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri ->
            runCatching { DiagnosticsFileWriter.write(context.contentResolver, uri, diagnostics) }
        }
    }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.data?.let(viewModel::saveTo)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val activity = context as? Activity
        val canAskAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } ?: true
        viewModel.onPermissionResult(granted, canAskAgain)
    }

    DisposableEffect(state.permission, lifecycleOwner, cameraSession, analyzer) {
        if (state.permission != CameraPermissionState.Granted) {
            onDispose { }
        } else {
            fun bindCamera() {
                viewModel.onCameraStarted()
                cameraSession.bind(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    onFrame = analyzer::analyze,
                    onError = viewModel::onCameraError,
                )
            }

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> bindCamera()
                    Lifecycle.Event.ON_STOP -> {
                        cameraSession.unbind()
                        viewModel.onHostStopped()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                bindCamera()
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                cameraSession.unbind()
                viewModel.onHostStopped()
            }
        }
    }

    DisposableEffect(cameraSession) {
        onDispose { cameraSession.close() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terima file") },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Arahkan kamera ke layar pengirim. Pastikan kecerahan layar tinggi, seluruh QR terlihat, dan jarak perangkat sekitar 15–30 cm.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Panduan kamera", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pegang perangkat stabil, hindari pantulan cahaya, dan tunggu sampai semua block pulih. Transfer berjalan offline dan tidak mengenkripsi isi file pada MVP.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .semantics { contentDescription = "Preview kamera penerima untuk membaca QR transfer" },
                contentAlignment = Alignment.Center,
            ) {
                if (state.permission == CameraPermissionState.Granted) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        when (state.permission) {
                            CameraPermissionState.Denied -> "Permission kamera belum diberikan. Tekan Izinkan kamera untuk mencoba lagi."
                            CameraPermissionState.PermanentlyDenied -> "Permission kamera diblokir. Buka Settings untuk mengizinkan kamera."
                            CameraPermissionState.Unknown -> "Kamera belum diminta. Tekan Izinkan kamera untuk memulai."
                            CameraPermissionState.Granted -> ""
                        },
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "${state.permission}: status permission kamera"
                        },
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = state.message
                },
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Diterima: ${state.receivedFrames} · Ditolak: ${state.rejectedFrames} · Duplikat: ${state.duplicateFrames}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    state.lastFrameKind?.let { kind ->
                        Text("Frame terakhir: $kind #${state.lastSequence}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            when (val reconstruction = state.reconstruction) {
                ReconstructionState.Idle -> Unit
                is ReconstructionState.Receiving -> Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Reconstruction", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${reconstruction.progress.recoveredBlocks}/${reconstruction.progress.totalBlocks} block",
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
                is ReconstructionState.ReadyToSave -> Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Checksum cocok", style = MaterialTheme.typography.titleMedium)
                        Text("${reconstruction.fileName} · ${reconstruction.sizeBytes} bytes")
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    type = reconstruction.mimeType
                                    putExtra(Intent.EXTRA_TITLE, reconstruction.fileName)
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }
                                saveLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Simpan file hasil transfer" },
                        ) { Text("Simpan file") }
                    }
                }
                is ReconstructionState.Saved -> Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Tersimpan: ${reconstruction.fileName} (${reconstruction.sizeBytes} bytes)",
                        modifier = Modifier.padding(16.dp).semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "File berhasil disimpan: ${reconstruction.fileName}"
                        },
                    )
                }
                is ReconstructionState.Failed -> Card(
                    modifier = Modifier.fillMaxWidth().semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "Error reconstruction: ${reconstruction.message}"
                    },
                ) {
                    Text(
                        "Reconstruction gagal: ${reconstruction.message}. Ulangi sesi atau periksa posisi kamera.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            ReceiveDiagnosticsSummaryCard(diagnostics)
            OutlinedButton(
                onClick = {
                    diagnosticsExporter.launch(
                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_TITLE, "lumen-diagnostics-receive.json")
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Export diagnostics penerimaan sebagai JSON" },
            ) { Text("Export diagnostics") }

            when (state.permission) {
                CameraPermissionState.Granted -> Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = viewModel::resetSession,
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Reset sesi penerimaan" },
                    ) { Text("Reset sesi") }
                    OutlinedButton(
                        onClick = { cameraSession.unbind(); viewModel.onHostStopped() },
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Hentikan kamera sementara" },
                    ) { Text("Stop kamera") }
                }
                else -> Button(
                    onClick = {
                        viewModel.markPermissionRequested()
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Izinkan akses kamera" },
                ) { Text("Izinkan kamera") }
            }
        }
    }
}

@Composable
private fun ReceiveDiagnosticsSummaryCard(diagnostics: TransferDiagnostics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
            Text("Elapsed: ${diagnostics.elapsedMs} ms", style = MaterialTheme.typography.bodyMedium)
            Text("Frame accepted: ${diagnostics.acceptedFrames}", style = MaterialTheme.typography.bodyMedium)
            Text("Duplicate/rejected: ${diagnostics.duplicateFrames}/${diagnostics.rejectedFrames}", style = MaterialTheme.typography.bodyMedium)
            Text("Goodput: ${"%.1f".format(diagnostics.goodputBytesPerSecond)} bytes/s", style = MaterialTheme.typography.bodyMedium)
            Text("Source/recovered blocks: ${diagnostics.sourceBlocks}/${diagnostics.recoveredBlocks}", style = MaterialTheme.typography.bodyMedium)
            Text("Equations: ${diagnostics.equationCount}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
