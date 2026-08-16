package com.ahlikomputerit.lumentransfer.domain.state

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import com.ahlikomputerit.lumentransfer.domain.model.TransferError

object TransferPhase {
    const val IDLE = "IDLE"
    const val PREPARING = "PREPARING"
    const val SENDING = "SENDING"
    const val PAUSED = "PAUSED"
    const val SCANNING = "SCANNING"
    const val RECEIVING = "RECEIVING"
    const val READY_TO_SAVE = "READY_TO_SAVE"
    const val SAVED = "SAVED"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}

enum class TransferRole { SEND, RECEIVE }

data class TransferState(
    val role: TransferRole,
    val phase: String = TransferPhase.IDLE,
    val transferId: TransferId? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long = 0L,
    val totalBlocks: Int = 0,
    val recoveredBlocks: Int = 0,
    val equationCount: Int = 0,
    val emittedFrames: Long = 0L,
    val lastSequence: Long? = null,
    val rotationDegrees: Int = 0,
    val lastActivityMs: Long = 0L,
    val timeoutDeadlineMs: Long? = null,
    val error: TransferError? = null,
    val message: String = "",
)

sealed interface TransferEvent {
    data class FilePrepared(val metadata: FileMetadata, val nowMs: Long) : TransferEvent
    data class SenderStarted(val nowMs: Long, val timeoutMs: Long) : TransferEvent
    data class SenderPaused(val nowMs: Long) : TransferEvent
    data class SenderResumed(val nowMs: Long, val timeoutMs: Long) : TransferEvent
    data class FrameEmitted(val kind: FrameKind, val sequence: Long, val nowMs: Long) : TransferEvent
    data class ReceiverStarted(val nowMs: Long, val timeoutMs: Long) : TransferEvent
    data class FrameAccepted(
        val transferId: TransferId,
        val kind: FrameKind,
        val sequence: Long,
        val recoveredBlocks: Int,
        val totalBlocks: Int,
        val equationCount: Int,
        val nowMs: Long,
    ) : TransferEvent
    data class ReadyToSave(val nowMs: Long) : TransferEvent
    data class Saved(val nowMs: Long) : TransferEvent
    data class RotationChanged(val degrees: Int, val nowMs: Long) : TransferEvent
    data class Timeout(val nowMs: Long) : TransferEvent
    data class Failed(val error: TransferError, val message: String, val nowMs: Long) : TransferEvent
    data class Cancelled(val nowMs: Long) : TransferEvent
    data class Reset(val nowMs: Long) : TransferEvent
}

fun reduceTransferState(state: TransferState, event: TransferEvent): TransferState = when (event) {
    is TransferEvent.FilePrepared -> state.copy(
        phase = TransferPhase.PREPARING,
        transferId = event.metadata.transferId,
        fileName = event.metadata.fileName,
        mimeType = event.metadata.mimeType,
        sizeBytes = event.metadata.sizeBytes,
        totalBlocks = event.metadata.sourceBlockCount,
        recoveredBlocks = 0,
        equationCount = 0,
        emittedFrames = 0,
        lastSequence = null,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = null,
        error = null,
        message = "File siap diproses",
    )
    is TransferEvent.SenderStarted -> state.copy(
        phase = TransferPhase.SENDING,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = event.nowMs + event.timeoutMs,
        error = null,
        message = "Mengirim frame QR",
    )
    is TransferEvent.SenderPaused -> state.copy(
        phase = TransferPhase.PAUSED,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = null,
        message = "Pengiriman dijeda",
    )
    is TransferEvent.SenderResumed -> state.copy(
        phase = TransferPhase.SENDING,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = event.nowMs + event.timeoutMs,
        message = "Pengiriman dilanjutkan",
    )
    is TransferEvent.FrameEmitted -> state.copy(
        phase = TransferPhase.SENDING,
        emittedFrames = state.emittedFrames + 1,
        lastSequence = event.sequence,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = state.timeoutDeadlineMs,
        message = "Frame ${event.sequence} ditampilkan",
    )
    is TransferEvent.ReceiverStarted -> state.copy(
        phase = TransferPhase.SCANNING,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = event.nowMs + event.timeoutMs,
        error = null,
        message = "Menunggu frame QR",
    )
    is TransferEvent.FrameAccepted -> state.copy(
        phase = TransferPhase.RECEIVING,
        transferId = state.transferId ?: event.transferId,
        recoveredBlocks = event.recoveredBlocks,
        totalBlocks = event.totalBlocks,
        equationCount = event.equationCount,
        lastSequence = event.sequence,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = state.timeoutDeadlineMs,
        message = "Frame ${event.sequence} diterima",
    )
    is TransferEvent.ReadyToSave -> state.copy(
        phase = TransferPhase.READY_TO_SAVE,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = null,
        message = "Checksum cocok; file siap disimpan",
    )
    is TransferEvent.Saved -> state.copy(
        phase = TransferPhase.SAVED,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = null,
        message = "File berhasil disimpan",
    )
    is TransferEvent.RotationChanged -> state.copy(
        rotationDegrees = ((event.degrees % 360) + 360) % 360,
        lastActivityMs = event.nowMs,
    )
    is TransferEvent.Timeout -> state.copy(
        phase = TransferPhase.FAILED,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = null,
        error = TransferError.DECODER_STALLED,
        message = "Transfer timeout karena tidak ada frame baru",
    )
    is TransferEvent.Failed -> state.copy(
        phase = TransferPhase.FAILED,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = null,
        error = event.error,
        message = event.message,
    )
    is TransferEvent.Cancelled -> state.copy(
        phase = TransferPhase.CANCELLED,
        lastActivityMs = event.nowMs,
        timeoutDeadlineMs = null,
        error = TransferError.SESSION_CANCELLED,
        message = "Sesi dibatalkan",
    )
    is TransferEvent.Reset -> TransferState(role = state.role, lastActivityMs = event.nowMs)
}
