package com.ahlikomputerit.lumentransfer.data.qr

import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import com.ahlikomputerit.lumentransfer.domain.protocol.FrameSerializer
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ZxingQrImageDecoderTest {
    @Test
    fun `rgba fixture decodes an encoded protocol frame`() {
        val frame = FrameEnvelope(
            version = 1,
            flags = 0,
            transferId = TransferId(ByteArray(16) { it.toByte() }),
            kind = FrameKind.META,
            seed = 0,
            degree = 0,
            sequence = 0,
            payload = "fixture".toByteArray(),
            frameCrc32 = 0u,
        )
        val encoded = FrameSerializer.serialize(frame)
        val matrix = ZxingQrEncoder().encode(encoded)
        val rgba = matrix.toRgba()

        val decoded = ZxingQrImageDecoder().decodeRgba(
            rgba = rgba,
            width = matrix.modules,
            height = matrix.modules,
            rowStride = matrix.modules * 4,
            pixelStride = 4,
            rotationDegrees = 0,
        )

        assertArrayEquals(encoded, decoded)
    }

    private fun QrMatrix.toRgba(): ByteArray {
        val output = ByteArray(modules * modules * 4)
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                val offset = (y * modules + x) * 4
                val value = if (isDark(x, y)) 0 else 0xFF
                output[offset] = 0xFF.toByte()
                output[offset + 1] = value.toByte()
                output[offset + 2] = value.toByte()
                output[offset + 3] = value.toByte()
            }
        }
        return output
    }
}
