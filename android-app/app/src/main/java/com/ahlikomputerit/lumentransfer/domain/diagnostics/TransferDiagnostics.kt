package com.ahlikomputerit.lumentransfer.domain.diagnostics

import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferError
import com.ahlikomputerit.lumentransfer.domain.state.TransferPhase
import com.ahlikomputerit.lumentransfer.domain.state.TransferRole

enum class DiagnosticsRejection {
    QR_NOT_FOUND,
    INVALID_PROTOCOL_FRAME,
    TRANSFER_ID_MISMATCH,
    OTHER,
}

/** Immutable, payload-free diagnostic snapshot for a single transfer session. */
data class TransferDiagnostics(
    val role: TransferRole,
    val startedAtMs: Long? = null,
    val endedAtMs: Long? = null,
    val emittedFrames: Long = 0L,
    val acceptedFrames: Long = 0L,
    val duplicateFrames: Long = 0L,
    val rejectedFrames: Long = 0L,
    val qrNotFoundFrames: Long = 0L,
    val invalidProtocolFrames: Long = 0L,
    val transferIdMismatchFrames: Long = 0L,
    val cameraFramesAnalyzed: Long = 0L,
    val lastCameraWidth: Int = 0,
    val lastCameraHeight: Int = 0,
    val lastCameraRowStride: Int = 0,
    val lastCameraPixelStride: Int = 0,
    val lastCameraRotationDegrees: Int = 0,
    val lastCameraBytes: Int = 0,
    val lastCameraLumaMin: Int = 0,
    val lastCameraLumaMax: Int = 0,
    val lastCameraLumaMean: Int = 0,
    val lastQrModules: Int = 0,
    val emittedBytes: Long = 0L,
    val acceptedBytes: Long = 0L,
    val systematicFrames: Long = 0L,
    val repairFrames: Long = 0L,
    val sourceBlocks: Int = 0,
    val recoveredBlocks: Int = 0,
    val equationCount: Int = 0,
    val lastSequence: Long? = null,
    val terminalPhase: String = TransferPhase.IDLE,
    val error: TransferError? = null,
) {
    val elapsedMs: Long
        get() = when {
            startedAtMs == null -> 0L
            endedAtMs != null -> (endedAtMs - startedAtMs).coerceAtLeast(0L)
            else -> 0L
        }

    val goodputBytesPerSecond: Double
        get() = if (elapsedMs <= 0L) 0.0 else acceptedBytes * 1_000.0 / elapsedMs
}

sealed interface DiagnosticsEvent {
    data class Started(val nowMs: Long) : DiagnosticsEvent
    data class FrameEmitted(val kind: FrameKind, val sequence: Long, val bytes: Int, val nowMs: Long) : DiagnosticsEvent
    data class FrameAccepted(
        val kind: FrameKind,
        val sequence: Long,
        val bytes: Int,
        val sourceBlocks: Int,
        val recoveredBlocks: Int,
        val equationCount: Int,
        val nowMs: Long,
    ) : DiagnosticsEvent
    data class CameraFrameObserved(
        val width: Int,
        val height: Int,
        val rowStride: Int,
        val pixelStride: Int,
        val rotationDegrees: Int,
        val bytes: Int,
        val lumaMin: Int,
        val lumaMax: Int,
        val lumaMean: Int,
        val nowMs: Long,
    ) : DiagnosticsEvent
    data class QrRendered(val modules: Int, val nowMs: Long) : DiagnosticsEvent
    data class Duplicate(val nowMs: Long) : DiagnosticsEvent
    data class Rejected(
        val nowMs: Long,
        val category: DiagnosticsRejection = DiagnosticsRejection.OTHER,
    ) : DiagnosticsEvent
    data class Completed(val nowMs: Long) : DiagnosticsEvent
    data class Failed(val error: TransferError, val nowMs: Long) : DiagnosticsEvent
    data class Cancelled(val nowMs: Long) : DiagnosticsEvent
    data class Reset(val nowMs: Long) : DiagnosticsEvent
}

fun reduceDiagnostics(state: TransferDiagnostics, event: DiagnosticsEvent): TransferDiagnostics = when (event) {
    is DiagnosticsEvent.Started -> state.copy(
        startedAtMs = event.nowMs,
        endedAtMs = null,
        terminalPhase = when (state.role) {
            TransferRole.SEND -> TransferPhase.SENDING
            TransferRole.RECEIVE -> TransferPhase.SCANNING
        },
        error = null,
    )
    is DiagnosticsEvent.FrameEmitted -> state.copy(
        emittedFrames = state.emittedFrames + 1,
        emittedBytes = state.emittedBytes + event.bytes,
        systematicFrames = state.systematicFrames + if (event.kind == FrameKind.SYSTEMATIC_DATA) 1 else 0,
        repairFrames = state.repairFrames + if (event.kind == FrameKind.REPAIR_DATA) 1 else 0,
        lastSequence = event.sequence,
    )
    is DiagnosticsEvent.FrameAccepted -> state.copy(
        acceptedFrames = state.acceptedFrames + 1,
        acceptedBytes = state.acceptedBytes + event.bytes,
        systematicFrames = state.systematicFrames + if (event.kind == FrameKind.SYSTEMATIC_DATA) 1 else 0,
        repairFrames = state.repairFrames + if (event.kind == FrameKind.REPAIR_DATA) 1 else 0,
        sourceBlocks = event.sourceBlocks,
        recoveredBlocks = event.recoveredBlocks,
        equationCount = event.equationCount,
        lastSequence = event.sequence,
        terminalPhase = TransferPhase.RECEIVING,
    )
    is DiagnosticsEvent.CameraFrameObserved -> state.copy(
        cameraFramesAnalyzed = state.cameraFramesAnalyzed + 1,
        lastCameraWidth = event.width,
        lastCameraHeight = event.height,
        lastCameraRowStride = event.rowStride,
        lastCameraPixelStride = event.pixelStride,
        lastCameraRotationDegrees = event.rotationDegrees,
        lastCameraBytes = event.bytes,
        lastCameraLumaMin = event.lumaMin,
        lastCameraLumaMax = event.lumaMax,
        lastCameraLumaMean = event.lumaMean,
    )
    is DiagnosticsEvent.QrRendered -> state.copy(lastQrModules = event.modules)
    is DiagnosticsEvent.Duplicate -> state.copy(duplicateFrames = state.duplicateFrames + 1)
    is DiagnosticsEvent.Rejected -> state.copy(
        rejectedFrames = state.rejectedFrames + 1,
        qrNotFoundFrames = state.qrNotFoundFrames + if (event.category == DiagnosticsRejection.QR_NOT_FOUND) 1 else 0,
        invalidProtocolFrames = state.invalidProtocolFrames + if (event.category == DiagnosticsRejection.INVALID_PROTOCOL_FRAME) 1 else 0,
        transferIdMismatchFrames = state.transferIdMismatchFrames + if (event.category == DiagnosticsRejection.TRANSFER_ID_MISMATCH) 1 else 0,
    )
    is DiagnosticsEvent.Completed -> state.copy(endedAtMs = event.nowMs, terminalPhase = TransferPhase.SAVED)
    is DiagnosticsEvent.Failed -> state.copy(endedAtMs = event.nowMs, terminalPhase = TransferPhase.FAILED, error = event.error)
    is DiagnosticsEvent.Cancelled -> state.copy(endedAtMs = event.nowMs, terminalPhase = TransferPhase.CANCELLED, error = TransferError.SESSION_CANCELLED)
    is DiagnosticsEvent.Reset -> TransferDiagnostics(role = state.role)
}
