package com.ahlikomputerit.lumentransfer.data.camera

import com.ahlikomputerit.lumentransfer.data.qr.QrImageDecoder
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import com.ahlikomputerit.lumentransfer.domain.protocol.FrameSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraFrameAnalyzerTest {
    private val sampleFrame = FrameEnvelope(
        version = 1,
        flags = 0,
        transferId = TransferId(ByteArray(16) { it.toByte() }),
        kind = FrameKind.META,
        seed = 0,
        degree = 0,
        sequence = 0,
        payload = "camera-fixture".toByteArray(),
        frameCrc32 = 0u,
    )

    @Test
    fun `valid decoded bytes are parsed and delivered`() {
        var delivered: FrameEnvelope? = null
        val rejected = mutableListOf<RejectionReason>()
        val analyzer = CameraFrameAnalyzer(
            qrDecoder = FakeDecoder(FrameSerializer.serialize(sampleFrame)),
            onEnvelope = { delivered = it },
            onRejected = rejected::add,
        )

        analyzer.analyze(dummyFrame())

        assertEquals(sampleFrame.transferId, delivered?.transferId)
        assertEquals(FrameKind.META, delivered?.kind)
        assertTrue(rejected.isEmpty())
    }

    @Test
    fun `missing qr becomes nonfatal rejection`() {
        val rejected = mutableListOf<RejectionReason>()
        CameraFrameAnalyzer(FakeDecoder(null), {}, rejected::add).analyze(dummyFrame())

        assertEquals(listOf(RejectionReason.QR_NOT_FOUND), rejected)
    }

    @Test
    fun `invalid protocol frame becomes nonfatal rejection`() {
        val rejected = mutableListOf<RejectionReason>()
        CameraFrameAnalyzer(FakeDecoder(byteArrayOf(0x00)), {}, rejected::add).analyze(dummyFrame())

        assertEquals(listOf(RejectionReason.INVALID_PROTOCOL_FRAME), rejected)
    }

    private fun dummyFrame() = CameraRgbaFrame(
        bytes = ByteArray(4),
        width = 1,
        height = 1,
        rowStride = 4,
        pixelStride = 4,
        rotationDegrees = 0,
    )

    private class FakeDecoder(private val result: ByteArray?) : QrImageDecoder {
        override fun decodeRgba(
            rgba: ByteArray,
            width: Int,
            height: Int,
            rowStride: Int,
            pixelStride: Int,
            rotationDegrees: Int,
        ): ByteArray? = result
    }
}
