package com.ahlikomputerit.lumentransfer.presentation.receive

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ahlikomputerit.lumentransfer.data.camera.RejectionReason
import com.ahlikomputerit.lumentransfer.data.file.DocumentSaver
import com.ahlikomputerit.lumentransfer.data.file.UnavailableDocumentSaver
import com.ahlikomputerit.lumentransfer.data.file.sanitizeDocumentName
import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import com.ahlikomputerit.lumentransfer.domain.protocol.FountainReconstructor
import com.ahlikomputerit.lumentransfer.domain.protocol.MetadataFrameCodec
import com.ahlikomputerit.lumentransfer.domain.protocol.ReconstructionProgress
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CameraPermissionState {
    data object Unknown : CameraPermissionState
    data object Granted : CameraPermissionState
    data object Denied : CameraPermissionState
    data object PermanentlyDenied : CameraPermissionState
}

sealed interface ReconstructionState {
    data object Idle : ReconstructionState
    data class Receiving(val progress: ReconstructionProgress) : ReconstructionState
    data class ReadyToSave(val fileName: String, val mimeType: String, val sizeBytes: Long) : ReconstructionState
    data class Saved(val fileName: String, val sizeBytes: Long) : ReconstructionState
    data class Failed(val message: String) : ReconstructionState
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
    val reconstruction: ReconstructionState = ReconstructionState.Idle,
)

class ReceiveViewModel(
    filesDir: File = File(System.getProperty("java.io.tmpdir"), "lumen-receive-default").apply { mkdirs() },
    private val documentSaver: DocumentSaver = UnavailableDocumentSaver,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()
    private val acceptedKeys = HashSet<String>()
    private val reconstructor = FountainReconstructor(filesDir)

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
        _uiState.update { it.copy(isScanning = true, message = "Scanning QR frame…") }
    }

    fun onCameraError(error: Throwable) {
        _uiState.update {
            it.copy(
                isScanning = false,
                message = "Kamera tidak tersedia: ${error.message ?: "unknown error"}",
            )
        }
    }

    fun onEnvelope(frame: FrameEnvelope) {
        val currentId = _uiState.value.activeTransferId
        if (currentId != null && currentId != frame.transferId) {
            onRejected(RejectionReason.TRANSFER_ID_MISMATCH)
            return
        }
        val key = "${frame.kind.wireValue}:${frame.sequence}"
        if (acceptedKeys.contains(key)) {
            onRejected(RejectionReason.DUPLICATE_FRAME)
            return
        }

        try {
            when (frame.kind) {
                FrameKind.META -> {
                    val metadata = MetadataFrameCodec.decode(frame.transferId, frame.payload)
                    reconstructor.acceptMetadata(metadata)
                    acceptedKeys.add(key)
                    updateForAcceptedFrame(frame, ReconstructionState.Receiving(reconstructor.progress()), metadata)
                    maybeVerifyCompleted()
                }
                FrameKind.SYSTEMATIC_DATA, FrameKind.REPAIR_DATA -> {
                    val progress = reconstructor.acceptData(frame)
                    acceptedKeys.add(key)
                    updateForAcceptedFrame(frame, ReconstructionState.Receiving(progress), reconstructor.currentMetadata())
                    maybeVerifyCompleted()
                }
                FrameKind.END -> {
                    acceptedKeys.add(key)
                    maybeVerifyCompleted()
                }
            }
        } catch (_: IllegalArgumentException) {
            onRejected(RejectionReason.INVALID_PROTOCOL_FRAME)
        }
    }

    fun saveTo(uri: Uri) {
        val ready = _uiState.value.reconstruction as? ReconstructionState.ReadyToSave ?: return
        val source = reconstructor.verifiedFile() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { documentSaver.save(source, uri) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            message = "File berhasil disimpan.",
                            reconstruction = ReconstructionState.Saved(ready.fileName, ready.sizeBytes),
                        )
                    }
                    reconstructor.cleanup()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(reconstruction = ReconstructionState.Failed(error.message ?: "Gagal menyimpan file"))
                    }
                }
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
        reconstructor.cleanup()
        _uiState.value = ReceiveUiState(permission = _uiState.value.permission)
    }

    override fun onCleared() {
        reconstructor.close()
        super.onCleared()
    }

    companion object {
        fun factory(filesDir: File, documentSaver: DocumentSaver): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ReceiveViewModel::class.java))
                    return ReceiveViewModel(filesDir, documentSaver) as T
                }
            }
    }

    private fun updateForAcceptedFrame(
        frame: FrameEnvelope,
        reconstruction: ReconstructionState,
        metadata: FileMetadata?,
    ) {
        _uiState.update {
            it.copy(
                activeTransferId = it.activeTransferId ?: frame.transferId,
                isScanning = true,
                message = metadata?.let { value -> "${value.fileName}: menerima frame…" }
                    ?: "Frame diterima.",
                receivedFrames = it.receivedFrames + 1,
                lastFrameKind = frame.kind,
                lastSequence = frame.sequence,
                reconstruction = reconstruction,
            )
        }
    }

    private fun maybeVerifyCompleted() {
        if (!reconstructor.isComplete()) return
        if (reconstructor.verify()) {
            val metadata = reconstructor.currentMetadata() ?: return
            _uiState.update {
                it.copy(
                    message = "Checksum cocok. Pilih lokasi untuk menyimpan file.",
                    reconstruction = ReconstructionState.ReadyToSave(
                        fileName = sanitizeDocumentName(metadata.fileName),
                        mimeType = metadata.mimeType,
                        sizeBytes = metadata.sizeBytes,
                    ),
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    message = "Checksum tidak cocok. Hasil sementara dibuang.",
                    reconstruction = ReconstructionState.Failed("INTEGRITY_MISMATCH"),
                )
            }
        }
    }
}
