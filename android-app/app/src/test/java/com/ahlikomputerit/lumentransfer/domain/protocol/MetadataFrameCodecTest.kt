package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MetadataFrameCodecTest {
    private val metadata = FileMetadata(
        transferId = TransferId(ByteArray(16) { it.toByte() }),
        fileName = "测试 file.bin",
        mimeType = "application/octet-stream",
        sizeBytes = 1_025,
        sha256 = ByteArray(32) { 0x22 },
        blockSize = 512,
        sourceBlockCount = 3,
    )

    @Test
    fun `metadata survives encode decode with unicode filename`() {
        val decoded = MetadataFrameCodec.decode(
            metadata.transferId,
            MetadataFrameCodec.encode(metadata),
        )

        assertEquals(metadata.fileName, decoded.fileName)
        assertEquals(metadata.mimeType, decoded.mimeType)
        assertEquals(metadata.sizeBytes, decoded.sizeBytes)
        assertEquals(metadata.blockSize, decoded.blockSize)
        assertEquals(metadata.sourceBlockCount, decoded.sourceBlockCount)
        assertArrayEquals(metadata.sha256, decoded.sha256)
    }

    @Test
    fun `invalid block count is rejected`() {
        val invalid = metadata.copy(sourceBlockCount = 2)
        assertThrows(IllegalArgumentException::class.java) {
            MetadataFrameCodec.decode(invalid.transferId, MetadataFrameCodec.encode(invalid))
        }
    }
}
