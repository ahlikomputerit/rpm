package com.ahlikomputerit.lumentransfer.presentation.receive

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CameraPermissionState {
    Unknown,
    Granted,
    Denied,
    PermanentlyDenied,
}

data class ReceiveUiState(
    val permission: CameraPermissionState = CameraPermissionState.Unknown,
    val isScanning: Boolean = false,
    val message: String = "Camera transfer is not implemented in this checkpoint.",
)

class ReceiveViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean = true) {
        _uiState.value = _uiState.value.copy(
            permission = when {
                granted -> CameraPermissionState.Granted
                canAskAgain -> CameraPermissionState.Denied
                else -> CameraPermissionState.PermanentlyDenied
            },
            isScanning = granted,
        )
    }

    fun markPermissionUnknown() {
        _uiState.value = _uiState.value.copy(permission = CameraPermissionState.Unknown)
    }
}
