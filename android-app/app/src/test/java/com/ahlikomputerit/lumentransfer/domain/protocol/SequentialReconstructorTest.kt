package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.integrity.Sha256Hasher
import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import java.io.File
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SequentialReconstructorTest {
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "lumen-reconstructor-test-${System.nanoTime()}")

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `out of order blocks reconstruct and verify`() {
        tempDir.mkdirs()
        val source = ByteArray(1_025) { (it * 13).toByte() }
        val metadata = metadata(source)
        val reconstructor = SequentialReconstructor(tempDir)
        reconstructor.acceptMetadata(metadata)

        val blockSize = metadata.blockSize
        val blocks = source.asList().chunked(blockSize).map { it.toByteArray() }
        reconstructor.acceptData(dataFrame(metadata, sequence = 3, payload = blocks[2]))
        reconstructor.acceptData(dataFrame(metadata, sequence = 1, payload = blocks[0]))
        reconstructor.acceptData(dataFrame(metadata, sequence = 2, payload = blocks[1]))

        assertTrue(reconstructor.isComplete())
        assertTrue(reconstructor.verify())
        val result = reconstructor.verifiedFile()!!.readBytes()
        assertArrayEquals(source, result)
        reconstructor.close()
    }

    @Test
    fun `checksum mismatch deletes temporary result`() {
        tempDir.mkdirs()
        val source = byteArrayOf(1, 2, 3)
        val metadata = metadata(source).copy(sha256 = ByteArray(32))
        val reconstructor = SequentialReconstructor(tempDir)
        reconstructor.acceptMetadata(metadata)
        reconstructor.acceptData(dataFrame(metadata, sequence = 1, payload = source))

        assertTrue(reconstructor.isComplete())
        assertFalse(reconstructor.verify())
        assertFalse(reconstructor.verifiedFile()?.exists() == true)
        reconstructor.close()
    }

    private fun metadata(source: ByteArray) = FileMetadata(
        transferId = TransferId(ByteArray(16) { it.toByte() }),
        fileName = "fixture.bin",
        mimeType = "application/octet-stream",
        sizeBytes = source.size.toLong(),
        sha256 = MessageDigest.getInstance("SHA-256").digest(source),
        blockSize = 512,
        sourceBlockCount = if (source.isEmpty()) 0 else (source.size + 511) / 512,
    )

    private fun dataFrame(metadata: FileMetadata, sequence: Long, payload: ByteArray) = FrameEnvelope(
        version = 1,
        flags = 0,
        transferId = metadata.transferId,
        kind = FrameKind.SYSTEMATIC_DATA,
        seed = sequence,
        degree = 1,
        sequence = sequence,
        payload = payload,
        frameCrc32 = 0u,
    )
}
