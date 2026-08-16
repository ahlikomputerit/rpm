package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

class FountainRoundTripTest {
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "lumen-fountain-roundtrip-${System.nanoTime()}")

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `source and reconstructor recover with dropped systematic frames`() {
        tempDir.mkdirs()
        val sourceBytes = ByteArray(16 * 64) { (it * 7).toByte() }
        val metadata = FileMetadata(
            transferId = TransferId(ByteArray(16) { (it + 9).toByte() }),
            fileName = "roundtrip.bin",
            mimeType = "application/octet-stream",
            sizeBytes = sourceBytes.size.toLong(),
            sha256 = MessageDigest.getInstance("SHA-256").digest(sourceBytes),
            blockSize = 64,
            sourceBlockCount = 16,
        )
        val source = FountainFrameSource(metadata) { ByteArrayInputStream(sourceBytes) }
        val reconstructor = FountainReconstructor(tempDir)
        val dropped = setOf(2, 5)

        while (true) {
            val frame = source.nextEnvelope()
            when {
                frame.kind == FrameKind.META -> reconstructor.acceptMetadata(metadata)
                frame.kind == FrameKind.SYSTEMATIC_DATA && frame.seed.toInt() in dropped -> Unit
                frame.kind == FrameKind.END -> break
                else -> reconstructor.acceptData(frame)
            }
        }

        assertTrue(reconstructor.isComplete())
        assertTrue(reconstructor.verify())
        reconstructor.close()
    }
}
