package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SequentialFrameSourceTest {
    private val metadata = FileMetadata(
        transferId = TransferId(ByteArray(16) { it.toByte() }),
        fileName = "sample.bin",
        mimeType = "application/octet-stream",
        sizeBytes = 2_300,
        sha256 = ByteArray(32) { 0x11 },
        blockSize = 512,
        sourceBlockCount = 5,
    )

    @Test
    fun `source emits metadata data chunks and end`() {
        val source = SequentialFrameSource(metadata) {
            ByteArrayInputStream(ByteArray(2_300) { (it % 251).toByte() })
        }

        val frames = buildList {
            repeat(7) { add(source.nextEnvelope()) }
        }

        assertEquals(FrameKind.META, frames.first().kind)
        assertEquals(FrameKind.END, frames.last().kind)
        assertEquals(5, frames.count { it.kind == FrameKind.SYSTEMATIC_DATA })
        assertTrue(frames.filter { it.kind == FrameKind.SYSTEMATIC_DATA }.all { it.payload.size <= 512 })
        assertEquals((0 until frames.size).toList(), frames.map { it.sequence.toInt() })
    }

    @Test
    fun `reset starts a new metadata loop`() {
        val source = SequentialFrameSource(metadata) { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
        assertEquals(FrameKind.META, source.nextEnvelope().kind)
        source.nextEnvelope()
        source.reset()
        assertEquals(FrameKind.META, source.nextEnvelope().kind)
    }
}
