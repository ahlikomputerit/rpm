package com.ahlikomputerit.lumentransfer.presentation.receive

import androidx.lifecycle.ViewModel
import com.ahlikomputerit.lumentransfer.data.camera.RejectionReason
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface CameraPermissionState {
    data object Unknown : CameraPermissionState
    data object Granted : CameraPermissionState
    data object Denied : CameraPermissionState
    data object PermanentlyDenied : CameraPermissionState
}

data class ReceiveUiState(
    val permission: CameraPermissionState = CameraPermissionState.Unknown,
    val isScanning: Boolean = false,
    val message: String = "Arahkan kamera ke QR sender.",
    val activeTransferId: TransferId? = null,
    val receivedFrames: Long = 0,
    val rejectedFrames: Long = 0,
    val duplicateFrames: Long = 0,
    val lastFrameKind: FrameKind? = null,
    val lastSequence: Long? = null,
)

class ReceiveViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()
    private val acceptedKeys = HashSet<String>()

    fun markPermissionRequested() {
        _uiState.update { it.copy(message = "Kamera diperlukan untuk membaca QR frame.") }
    }

    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean = true) {
        _uiState.update {
            it.copy(
                permission = when {
                    granted -> CameraPermissionState.Granted
                    canAskAgain -> CameraPermissionState.Denied
                    else -> CameraPermissionState.PermanentlyDenied
                },
                isScanning = granted,
                message = if (granted) "Scanning QR frame…" else "Permission kamera belum diberikan.",
            )
        }
    }

    fun onCameraStarted() {
        _uiState.update {
            it.copy(isScanning = true, message = "Scanning QR frame…")
        }
    }

    fun onCameraError(error: Throwable) {
        _uiState.update {
            it.copy(
                isScanning = false,
                message = "Kamera tidak tersedia: ${error.message ?: "unknown error"}",
            )
        }
    }

    fun onCameraFrame(frame: com.ahlikomputerit.lumentransfer.data.camera.CameraRgbaFrame) {
        // CameraFrameAnalyzer performs decoding and calls onEnvelope/onRejected.
    }

    fun onEnvelope(frame: FrameEnvelope) {
        val currentId = _uiState.value.activeTransferId
        if (currentId != null && currentId != frame.transferId) {
            onRejected(RejectionReason.TRANSFER_ID_MISMATCH)
            return
        }
        val key = "${frame.kind.wireValue}:${frame.sequence}"
        if (!acceptedKeys.add(key)) {
            onRejected(RejectionReason.DUPLICATE_FRAME)
            return
        }
        _uiState.update {
            it.copy(
                activeTransferId = currentId ?: frame.transferId,
                isScanning = true,
                message = "Frame diterima. Reconstruction akan ditambahkan pada tahap berikutnya.",
                receivedFrames = it.receivedFrames + 1,
                lastFrameKind = frame.kind,
                lastSequence = frame.sequence,
            )
        }
    }

    fun onRejected(reason: RejectionReason) {
        _uiState.update {
            when (reason) {
                RejectionReason.DUPLICATE_FRAME -> it.copy(duplicateFrames = it.duplicateFrames + 1)
                RejectionReason.TRANSFER_ID_MISMATCH -> it.copy(
                    rejectedFrames = it.rejectedFrames + 1,
                    message = "Frame berasal dari transfer lain; diabaikan.",
                )
                RejectionReason.QR_NOT_FOUND -> it.copy(rejectedFrames = it.rejectedFrames + 1)
                RejectionReason.INVALID_PROTOCOL_FRAME -> it.copy(rejectedFrames = it.rejectedFrames + 1)
            }
        }
    }

    fun resetSession() {
        acceptedKeys.clear()
        _uiState.value = ReceiveUiState(permission = _uiState.value.permission)
    }
}
