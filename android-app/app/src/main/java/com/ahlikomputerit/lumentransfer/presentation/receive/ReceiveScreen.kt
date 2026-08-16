package com.ahlikomputerit.lumentransfer.presentation.receive

import android.Manifest
import android.content.Intent
import android.app.Activity
import android.content.pm.PackageManager
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ahlikomputerit.lumentransfer.data.camera.CameraFrameAnalyzer
import com.ahlikomputerit.lumentransfer.data.camera.CameraXSession
import com.ahlikomputerit.lumentransfer.data.qr.ZxingQrImageDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()
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
        if (state.permission == CameraPermissionState.Granted) {
            viewModel.onCameraStarted()
            cameraSession.bind(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                onFrame = analyzer::analyze,
                onError = viewModel::onCameraError,
            )
        }
        onDispose { cameraSession.unbind() }
    }

    DisposableEffect(cameraSession) {
        onDispose { cameraSession.close() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terima file") },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Arahkan kamera ke layar pengirim. Frame yang lolos CRC akan diteruskan ke parser protocol.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .semantics { contentDescription = "Camera preview" },
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
                            CameraPermissionState.Denied -> "Permission kamera belum diberikan"
                            CameraPermissionState.PermanentlyDenied -> "Permission kamera diblokir di Settings"
                            CameraPermissionState.Unknown -> "Kamera belum diminta"
                            CameraPermissionState.Granted -> ""
                        },
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
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
                        Text("${reconstruction.progress.recoveredBlocks}/${reconstruction.progress.totalBlocks} block")
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
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Simpan file") }
                    }
                }
                is ReconstructionState.Saved -> Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Tersimpan: ${reconstruction.fileName} (${reconstruction.sizeBytes} bytes)",
                        modifier = Modifier.padding(16.dp),
                    )
                }
                is ReconstructionState.Failed -> Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Reconstruction gagal: ${reconstruction.message}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            when (state.permission) {
                CameraPermissionState.Granted -> Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(onClick = viewModel::resetSession, modifier = Modifier.weight(1f)) {
                        Text("Reset sesi")
                    }
                    OutlinedButton(onClick = { cameraSession.unbind() }, modifier = Modifier.weight(1f)) {
                        Text("Stop kamera")
                    }
                }
                else -> Button(
                    onClick = {
                        viewModel.markPermissionRequested()
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Izinkan kamera") }
            }
        }
    }
}
