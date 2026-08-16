package com.ahlikomputerit.lumentransfer.presentation.receive

import android.Manifest
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ahlikomputerit.lumentransfer.data.camera.RejectionReason
import com.ahlikomputerit.lumentransfer.data.file.DiagnosticsFileWriter
import com.ahlikomputerit.lumentransfer.data.file.DocumentSaver
import com.ahlikomputerit.lumentransfer.data.file.UnavailableDocumentSaver
import com.ahlikomputerit.lumentransfer.data.file.sanitizeDocumentName
import com.ahlikomputerit.lumentransfer.domain.diagnostics.DiagnosticsEvent
import com.ahlikomputerit.lumentransfer.domain.diagnostics.DiagnosticsRejection
import com.ahlikomputerit.lumentransfer.domain.diagnostics.DiagnosticsStore
import com.ahlikomputerit.lumentransfer.domain.diagnostics.TransferDiagnostics
import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferError
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import com.ahlikomputerit.lumentransfer.domain.protocol.FountainReconstructor
import com.ahlikomputerit.lumentransfer.domain.protocol.MetadataFrameCodec
import com.ahlikomputerit.lumentransfer.domain.protocol.ReconstructionProgress
import com.ahlikomputerit.lumentransfer.domain.state.TransferEvent
import com.ahlikomputerit.lumentransfer.domain.state.TransferPhase
import com.ahlikomputerit.lumentransfer.domain.state.TransferRole
import com.ahlikomputerit.lumentransfer.domain.state.TransferState
import com.ahlikomputerit.lumentransfer.domain.state.TransferStore
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    private val contentResolver: ContentResolver? = null,
    private val timeoutDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()
    private val acceptedKeys = HashSet<String>()
    private val reconstructor = FountainReconstructor(filesDir)
    private val transferStore = TransferStore(TransferState(TransferRole.RECEIVE))
    val transferState: StateFlow<TransferState> = transferStore.state
    private val diagnosticsStore = DiagnosticsStore(TransferRole.RECEIVE)
    val diagnostics: StateFlow<TransferDiagnostics> = diagnosticsStore.snapshot
    private val timeoutScope = CoroutineScope(SupervisorJob() + timeoutDispatcher)
    private var timeoutJob: Job? = null

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
        if (!granted) {
            timeoutJob?.cancel()
            val failedAt = now()
            diagnosticsStore.dispatch(DiagnosticsEvent.Failed(TransferError.CAMERA_PERMISSION_DENIED, failedAt))
            transferStore.dispatch(TransferEvent.Failed(TransferError.CAMERA_PERMISSION_DENIED, "Permission kamera belum diberikan", failedAt))
        }
    }

    fun onCameraStarted() {
        val startedAt = now()
        diagnosticsStore.dispatch(DiagnosticsEvent.Started(startedAt))
        _uiState.update { it.copy(isScanning = true, message = "Scanning QR frame…") }
        transferStore.dispatch(TransferEvent.ReceiverStarted(startedAt, SESSION_TIMEOUT_MS))
        startTimeoutWatch()
    }

    fun onCameraError(error: Throwable) {
        timeoutJob?.cancel()
        val failedAt = now()
        diagnosticsStore.dispatch(DiagnosticsEvent.Failed(TransferError.CAMERA_UNAVAILABLE, failedAt))
        transferStore.dispatch(TransferEvent.Failed(TransferError.CAMERA_UNAVAILABLE, error.message ?: "Camera unavailable", failedAt))
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
                    transferStore.dispatch(TransferEvent.FilePrepared(metadata, now()))
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
                    val completedAt = now()
                    diagnosticsStore.dispatch(DiagnosticsEvent.Completed(completedAt))
                    transferStore.dispatch(TransferEvent.Saved(completedAt))
                    _uiState.update {
                        it.copy(
                            message = "File berhasil disimpan.",
                            reconstruction = ReconstructionState.Saved(ready.fileName, ready.sizeBytes),
                        )
                    }
                    reconstructor.cleanup()
                }
                .onFailure { error ->
                    val failedAt = now()
                    diagnosticsStore.dispatch(DiagnosticsEvent.Failed(TransferError.STORAGE_WRITE_FAILED, failedAt))
                    transferStore.dispatch(TransferEvent.Failed(TransferError.STORAGE_WRITE_FAILED, error.message ?: "Gagal menyimpan file", failedAt))
                    _uiState.update {
                        it.copy(reconstruction = ReconstructionState.Failed(error.message ?: "Gagal menyimpan file"))
                    }
                }
        }
    }

    fun onRejected(reason: RejectionReason) {
        val diagnosticEvent = when (reason) {
            RejectionReason.DUPLICATE_FRAME -> DiagnosticsEvent.Duplicate(now())
            RejectionReason.QR_NOT_FOUND -> DiagnosticsEvent.Rejected(now(), DiagnosticsRejection.QR_NOT_FOUND)
            RejectionReason.INVALID_PROTOCOL_FRAME -> DiagnosticsEvent.Rejected(now(), DiagnosticsRejection.INVALID_PROTOCOL_FRAME)
            RejectionReason.TRANSFER_ID_MISMATCH -> DiagnosticsEvent.Rejected(now(), DiagnosticsRejection.TRANSFER_ID_MISMATCH)
        }
        diagnosticsStore.dispatch(diagnosticEvent)
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
        timeoutJob?.cancel()
        acceptedKeys.clear()
        reconstructor.cleanup()
        val resetAt = now()
        diagnosticsStore.dispatch(DiagnosticsEvent.Reset(resetAt))
        transferStore.dispatch(TransferEvent.Reset(resetAt))
        _uiState.value = ReceiveUiState(permission = _uiState.value.permission)
    }

    fun cancelSession() {
        timeoutJob?.cancel()
        val cancelledAt = now()
        diagnosticsStore.dispatch(DiagnosticsEvent.Cancelled(cancelledAt))
        transferStore.dispatch(TransferEvent.Cancelled(cancelledAt))
        acceptedKeys.clear()
        reconstructor.cleanup()
        _uiState.update { it.copy(isScanning = false, reconstruction = ReconstructionState.Failed("SESSION_CANCELLED")) }
    }

    fun onRotationChanged(degrees: Int) {
        transferStore.dispatch(TransferEvent.RotationChanged(degrees, now()))
    }

    fun onHostStopped() {
        timeoutJob?.cancel()
        if (_uiState.value.isScanning) {
            _uiState.update {
                it.copy(
                    isScanning = false,
                    message = "Scanning dijeda saat aplikasi tidak terlihat. Kembali ke layar ini untuk melanjutkan.",
                )
            }
        }
    }

    fun saveDiagnostics(uri: Uri) {
        val resolver = contentResolver ?: return
        val snapshot = diagnostics.value
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { DiagnosticsFileWriter.write(resolver, uri, snapshot) }
                .onFailure { error ->
                    _uiState.update { it.copy(message = "Gagal menyimpan diagnostics: ${error.message ?: "unknown error"}") }
                }
        }
    }

    override fun onCleared() {
        timeoutJob?.cancel()
        timeoutScope.cancel()
        reconstructor.close()
        super.onCleared()
    }

    companion object {
        fun factory(filesDir: File, documentSaver: DocumentSaver, contentResolver: ContentResolver? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ReceiveViewModel::class.java))
                    return ReceiveViewModel(filesDir, documentSaver, contentResolver) as T
                }
            }

        private const val SESSION_TIMEOUT_MS = 30_000L
    }

    private fun updateForAcceptedFrame(
        frame: FrameEnvelope,
        reconstruction: ReconstructionState,
        metadata: FileMetadata?,
    ) {
        val progress = (reconstruction as? ReconstructionState.Receiving)?.progress
        diagnosticsStore.dispatch(
            DiagnosticsEvent.FrameAccepted(
                kind = frame.kind,
                sequence = frame.sequence,
                bytes = frame.payload.size,
                sourceBlocks = progress?.totalBlocks ?: metadata?.sourceBlockCount ?: 0,
                recoveredBlocks = progress?.recoveredBlocks ?: 0,
                equationCount = progress?.equationCount ?: 0,
                nowMs = now(),
            ),
        )
        transferStore.dispatch(
            TransferEvent.FrameAccepted(
                transferId = frame.transferId,
                kind = frame.kind,
                sequence = frame.sequence,
                recoveredBlocks = progress?.recoveredBlocks ?: 0,
                totalBlocks = progress?.totalBlocks ?: metadata?.sourceBlockCount ?: 0,
                equationCount = progress?.equationCount ?: 0,
                nowMs = now(),
            ),
        )
        startTimeoutWatch()
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
            transferStore.dispatch(TransferEvent.ReadyToSave(now()))
            timeoutJob?.cancel()
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
            val failedAt = now()
            diagnosticsStore.dispatch(DiagnosticsEvent.Failed(TransferError.INTEGRITY_MISMATCH, failedAt))
            transferStore.dispatch(TransferEvent.Failed(TransferError.INTEGRITY_MISMATCH, "Checksum tidak cocok", failedAt))
            _uiState.update {
                it.copy(
                    message = "Checksum tidak cocok. Hasil sementara dibuang.",
                    reconstruction = ReconstructionState.Failed("INTEGRITY_MISMATCH"),
                )
            }
        }
    }

    private fun startTimeoutWatch() {
        timeoutJob?.cancel()
        timeoutJob = timeoutScope.launch {
            delay(SESSION_TIMEOUT_MS)
            val state = transferState.value
            if (state.phase == TransferPhase.SCANNING || state.phase == TransferPhase.RECEIVING) {
                val timeoutAt = now()
                diagnosticsStore.dispatch(DiagnosticsEvent.Failed(TransferError.DECODER_STALLED, timeoutAt))
                transferStore.dispatch(TransferEvent.Timeout(timeoutAt))
                _uiState.update { it.copy(isScanning = false, message = "Transfer timeout karena tidak ada frame baru") }
            }
        }
    }

    private fun now(): Long = System.currentTimeMillis()

}
