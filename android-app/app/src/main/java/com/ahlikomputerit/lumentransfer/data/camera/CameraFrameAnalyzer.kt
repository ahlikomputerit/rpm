package com.ahlikomputerit.lumentransfer.data.camera

import com.ahlikomputerit.lumentransfer.data.qr.QrImageDecoder
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.protocol.FrameSerializer
import com.ahlikomputerit.lumentransfer.domain.protocol.ProtocolException

class CameraFrameAnalyzer(
    private val qrDecoder: QrImageDecoder,
    private val onEnvelope: (FrameEnvelope) -> Unit,
    private val onRejected: (RejectionReason) -> Unit,
) {
    fun analyze(frame: CameraRgbaFrame) {
        val bytes = qrDecoder.decodeRgba(
            rgba = frame.bytes,
            width = frame.width,
            height = frame.height,
            rowStride = frame.rowStride,
            pixelStride = frame.pixelStride,
            rotationDegrees = frame.rotationDegrees,
        ) ?: run {
            onRejected(RejectionReason.QR_NOT_FOUND)
            return
        }
        try {
            onEnvelope(FrameSerializer.parse(bytes))
        } catch (_: ProtocolException) {
            onRejected(RejectionReason.INVALID_PROTOCOL_FRAME)
        } catch (_: IllegalArgumentException) {
            onRejected(RejectionReason.INVALID_PROTOCOL_FRAME)
        }
    }
}

enum class RejectionReason {
    QR_NOT_FOUND,
    INVALID_PROTOCOL_FRAME,
    DUPLICATE_FRAME,
    TRANSFER_ID_MISMATCH,
}
