package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FrameSerializerTest {
    private val sample = FrameEnvelope(
        version = ProtocolConstants.VERSION,
        flags = 0,
        transferId = TransferId(ByteArray(16) { it.toByte() }),
        kind = FrameKind.SYSTEMATIC_DATA,
        seed = 42,
        degree = 1,
        sequence = 7,
        payload = "hello-lumen".toByteArray(),
        frameCrc32 = 0u,
    )

    @Test
    fun `round trip preserves frame values`() {
        val encoded = FrameSerializer.serialize(sample)
        val decoded = FrameSerializer.parse(encoded)

        assertEquals(sample.version, decoded.version)
        assertEquals(sample.flags, decoded.flags)
        assertEquals(sample.transferId, decoded.transferId)
        assertEquals(sample.kind, decoded.kind)
        assertEquals(sample.seed, decoded.seed)
        assertEquals(sample.degree, decoded.degree)
        assertEquals(sample.sequence, decoded.sequence)
        assertArrayEquals(sample.payload, decoded.payload)
        assertEquals(true, decoded.frameCrc32 != 0u)
    }

    @Test
    fun `invalid magic is rejected`() {
        val encoded = FrameSerializer.serialize(sample).also { it[0] = 0x00 }
        assertThrows(ProtocolException::class.java) { FrameSerializer.parse(encoded) }
    }

    @Test
    fun `unsupported version is rejected`() {
        val encoded = FrameSerializer.serialize(sample).also { it[2] = 0x7F }
        assertThrows(ProtocolException::class.java) { FrameSerializer.parse(encoded) }
    }

    @Test
    fun `truncated payload is rejected`() {
        val encoded = FrameSerializer.serialize(sample).copyOfRange(0, 10)
        assertThrows(ProtocolException::class.java) { FrameSerializer.parse(encoded) }
    }

    @Test
    fun `crc mismatch is rejected`() {
        val encoded = FrameSerializer.serialize(sample)
        encoded[ProtocolConstants.HEADER_SIZE] = (encoded[ProtocolConstants.HEADER_SIZE].toInt() xor 0x01).toByte()
        assertThrows(ProtocolException::class.java) { FrameSerializer.parse(encoded) }
    }
}
