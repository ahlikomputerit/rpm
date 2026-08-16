package com.ahlikomputerit.lumentransfer.presentation.receive

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    viewModel: ReceiveViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onPermissionResult(granted) }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.onPermissionResult(true)
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
                "Arahkan kamera ke layar pengirim. Decoder QR real-time akan diaktifkan setelah camera adapter selesai.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .semantics { contentDescription = "Camera preview placeholder" },
                contentAlignment = Alignment.Center,
            ) {
                when (state.permission) {
                    CameraPermissionState.Granted -> Text("Permission kamera aktif\nPreview CameraX belum diaktifkan")
                    CameraPermissionState.Denied -> Text("Permission kamera belum diberikan")
                    CameraPermissionState.PermanentlyDenied -> Text("Permission kamera diblokir di Settings")
                    CameraPermissionState.Unknown -> Text("Kamera belum diminta")
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = state.message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            when (state.permission) {
                CameraPermissionState.Granted -> OutlinedButton(
                    onClick = { viewModel.markPermissionUnknown() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Periksa ulang permission") }
                else -> Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Izinkan kamera") }
            }
        }
    }
}
