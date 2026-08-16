package com.ahlikomputerit.lumentransfer.data.camera

import com.ahlikomputerit.lumentransfer.data.qr.QrImageDecoder
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.protocol.FrameSerializer
import com.ahlikomputerit.lumentransfer.domain.protocol.ProtocolException

class CameraFrameAnalyzer(
    private val qrDecoder: QrImageDecoder,
    private val onEnvelope: (FrameEnvelope) -> Unit,
    private val onRejected: (RejectionReason) -> Unit,
    private val onFrameObserved: (CameraRgbaFrame, Int, Int, Int) -> Unit = { _, _, _, _ -> },
) {
    private var analyzedFrames = 0L

    fun analyze(frame: CameraRgbaFrame) {
        analyzedFrames += 1
        if (analyzedFrames == 1L || analyzedFrames % OBSERVATION_INTERVAL == 0L) {
            val luma = sampleLuminance(frame)
            onFrameObserved(frame, luma.min, luma.max, luma.mean)
        }
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

    private fun sampleLuminance(frame: CameraRgbaFrame): LumaSample {
        var min = 255
        var max = 0
        var sum = 0L
        var count = 0
        val yStep = maxOf(1, frame.height / 32)
        val xStep = maxOf(1, frame.width / 32)
        for (y in 0 until frame.height step yStep) {
            for (x in 0 until frame.width step xStep) {
                val offset = y * frame.rowStride + x * frame.pixelStride
                if (offset < 0 || offset + 3 >= frame.bytes.size) continue
                val red = frame.bytes[offset + 1].toInt() and 0xFF
                val green = frame.bytes[offset + 2].toInt() and 0xFF
                val blue = frame.bytes[offset + 3].toInt() and 0xFF
                val luma = (299 * red + 587 * green + 114 * blue) / 1_000
                min = minOf(min, luma)
                max = maxOf(max, luma)
                sum += luma
                count += 1
            }
        }
        return if (count == 0) LumaSample(0, 0, 0) else LumaSample(min, max, (sum / count).toInt())
    }

    private data class LumaSample(val min: Int, val max: Int, val mean: Int)

    companion object {
        private const val OBSERVATION_INTERVAL = 30L
    }
}

enum class RejectionReason {
    QR_NOT_FOUND,
    INVALID_PROTOCOL_FRAME,
    DUPLICATE_FRAME,
    TRANSFER_ID_MISMATCH,
}
