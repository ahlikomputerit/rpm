package com.ahlikomputerit.lumentransfer.data.qr

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZxingQrCodecTest {
    private val encoder = ZxingQrEncoder()
    private val decoder = ZxingQrDecoder()

    @Test
    fun `ascii payload survives qr round trip`() {
        val payload = "LT|frame|hello".toByteArray()
        val matrix = encoder.encode(payload)

        assertTrue(matrix.modules > 0)
        assertArrayEquals(payload, decoder.decode(matrix))
    }

    @Test
    fun `binary payload survives url safe transport round trip`() {
        val payload = ByteArray(256) { index -> (index * 37).toByte() }
        val matrix = encoder.encode(payload)

        assertArrayEquals(payload, decoder.decode(matrix))
    }
}
