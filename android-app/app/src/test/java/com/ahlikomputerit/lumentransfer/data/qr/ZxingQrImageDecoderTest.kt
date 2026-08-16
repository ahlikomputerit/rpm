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

    @Test
    fun `camera frame with surrounding preview decodes without pure barcode hint`() {
        val frame = FrameEnvelope(
            version = 1,
            flags = 0,
            transferId = TransferId(ByteArray(16) { it.toByte() }),
            kind = FrameKind.META,
            seed = 0,
            degree = 0,
            sequence = 1,
            payload = ByteArray(1_024) { index -> (index * 31).toByte() },
            frameCrc32 = 0u,
        )
        val encoded = FrameSerializer.serialize(frame)
        val matrix = ZxingQrEncoder().encode(encoded)
        val width = 960
        val height = 720
        val decoded = ZxingQrImageDecoder().decodeRgba(
            rgba = matrix.toEmbeddedRgba(width, height, scale = 5),
            width = width,
            height = height,
            rowStride = width * 4,
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

    private fun QrMatrix.toEmbeddedRgba(width: Int, height: Int, scale: Int): ByteArray {
        val output = ByteArray(width * height * 4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = (y * width + x) * 4
                output[offset] = 0xFF.toByte()
                output[offset + 1] = 0xFF.toByte()
                output[offset + 2] = 0xFF.toByte()
                output[offset + 3] = 0xFF.toByte()
            }
        }
        val left = (width - modules * scale) / 2
        val top = (height - modules * scale) / 2
        for (moduleY in 0 until modules) {
            for (moduleX in 0 until modules) {
                if (!isDark(moduleX, moduleY)) continue
                for (dy in 0 until scale) {
                    for (dx in 0 until scale) {
                        val x = left + moduleX * scale + dx
                        val y = top + moduleY * scale + dy
                        val offset = (y * width + x) * 4
                        output[offset + 1] = 0
                        output[offset + 2] = 0
                        output[offset + 3] = 0
                    }
                }
            }
        }
        return output
    }
}
