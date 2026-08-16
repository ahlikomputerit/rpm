package com.ahlikomputerit.lumentransfer.presentation.send

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ahlikomputerit.lumentransfer.data.file.AndroidDocumentReader
import com.ahlikomputerit.lumentransfer.data.qr.QrMatrix
import com.ahlikomputerit.lumentransfer.data.qr.ZxingQrEncoder
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.protocol.FrameSerializer
import com.ahlikomputerit.lumentransfer.domain.protocol.SequentialFrameSource
import com.ahlikomputerit.lumentransfer.domain.runtime.NoOpScreenOnPolicy
import com.ahlikomputerit.lumentransfer.domain.runtime.ScreenOnPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SendStatus {
    data object Idle : SendStatus
    data object Reading : SendStatus
    data object Ready : SendStatus
    data class Failed(val message: String) : SendStatus
}

sealed interface SenderStatus {
    data object Idle : SenderStatus
    data class Running(val frameNumber: Long, val kind: FrameKind) : SenderStatus
    data class Paused(val frameNumber: Long) : SenderStatus
}

data class SendUiState(
    val status: SendStatus = SendStatus.Idle,
    val selected: AndroidDocumentReader.SelectedDocument? = null,
    val qrPreview: QrMatrix? = null,
    val senderStatus: SenderStatus = SenderStatus.Idle,
)

class SendViewModel(
    contentResolver: ContentResolver,
    private val screenOnPolicy: ScreenOnPolicy = NoOpScreenOnPolicy(),
) : ViewModel() {
    private val reader = AndroidDocumentReader(contentResolver)
    private val qrEncoder = ZxingQrEncoder()
    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    private var senderJob: Job? = null
    private var frameSource: SequentialFrameSource? = null

    fun onFileSelected(uri: Uri) {
        cancelSending()
        _uiState.value = SendUiState(status = SendStatus.Reading)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val document = reader.read(uri)
                val previewBytes = reader.readPrefix(uri, 512)
                val previewFrame = FrameEnvelope(
                    version = 1,
                    flags = 0,
                    transferId = document.metadata.transferId,
                    kind = FrameKind.SYSTEMATIC_DATA,
                    seed = 0,
                    degree = 1,
                    sequence = 0,
                    payload = previewBytes,
                    frameCrc32 = 0u,
                )
                document to qrEncoder.encode(FrameSerializer.serialize(previewFrame))
            }.onSuccess { (document, qrPreview) ->
                _uiState.value = SendUiState(
                    status = SendStatus.Ready,
                    selected = document,
                    qrPreview = qrPreview,
                )
            }.onFailure { error ->
                _uiState.value = SendUiState(
                    status = SendStatus.Failed(error.message ?: "Unable to prepare selected file"),
                )
            }
        }
    }

    fun startSending() {
        val selected = _uiState.value.selected ?: return
        if (senderJob?.isActive == true) return

        if (frameSource == null) {
            frameSource = SequentialFrameSource(selected.metadata) { reader.open(selected.uri) }
        }
        screenOnPolicy.acquire()
        _uiState.update { it.copy(senderStatus = SenderStatus.Running(0, FrameKind.META)) }
        senderJob = viewModelScope.launch(Dispatchers.Default) {
            var frameNumber = (_uiState.value.senderStatus as? SenderStatus.Paused)?.frameNumber ?: 0L
            try {
                while (true) {
                    val envelope = frameSource?.nextEnvelope() ?: break
                    val qr = qrEncoder.encode(FrameSerializer.serialize(envelope))
                    frameNumber += 1
                    _uiState.update {
                        it.copy(
                            qrPreview = qr,
                            senderStatus = SenderStatus.Running(frameNumber, envelope.kind),
                        )
                    }
                    if (envelope.kind == FrameKind.END) frameSource?.reset()
                    delay(FRAME_INTERVAL_MS)
                }
            } catch (_: CancellationException) {
                // Pause/cancel is an expected lifecycle event.
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(senderStatus = SenderStatus.Idle, status = SendStatus.Failed(error.message ?: "Sender failed"))
                }
            } finally {
                screenOnPolicy.release()
            }
        }
    }

    fun pauseSending() {
        val current = _uiState.value.senderStatus
        val frameNumber = when (current) {
            is SenderStatus.Running -> current.frameNumber
            is SenderStatus.Paused -> current.frameNumber
            SenderStatus.Idle -> return
        }
        _uiState.update { it.copy(senderStatus = SenderStatus.Paused(frameNumber)) }
        senderJob?.cancel()
        senderJob = null
        screenOnPolicy.release()
    }

    fun cancelSending() {
        senderJob?.cancel()
        senderJob = null
        frameSource?.close()
        frameSource = null
        screenOnPolicy.release()
        _uiState.update { it.copy(senderStatus = SenderStatus.Idle) }
    }

    fun clearSelection() {
        cancelSending()
        _uiState.value = SendUiState()
    }

    override fun onCleared() {
        cancelSending()
        super.onCleared()
    }

    companion object {
        private const val FRAME_INTERVAL_MS = 120L

        fun factory(
            contentResolver: ContentResolver,
            screenOnPolicy: ScreenOnPolicy = NoOpScreenOnPolicy(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SendViewModel::class.java))
                return SendViewModel(contentResolver, screenOnPolicy) as T
            }
        }
    }
}
