package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FountainDecoderTest {
    @Test
    fun `repair frame recovers dropped systematic block`() {
        val blocks = List(8) { index -> ByteArray(32) { (index * 17 + it).toByte() } }
        val metadata = metadata(blocks)
        val decoder = FountainDecoder(metadata)

        blocks.dropLast(1).forEachIndexed { index, payload ->
            decoder.accept(systematic(metadata.transferId, index, payload, index.toLong()))
        }
        val repairSeed = (0L..10_000L).first { seed ->
            FountainCodec.degree(seed, blocks.size) == 1 &&
                FountainCodec.chooseIndices(seed, blocks.size, 1).single() == blocks.lastIndex
        }
        decoder.accept(
            repair(
                metadata.transferId,
                repairSeed,
                blocks.last(),
                sequence = 99,
            ),
        )

        assertTrue(decoder.isComplete())
        blocks.indices.forEach { index -> assertArrayEquals(blocks[index], decoder.block(index)) }
    }

    @Test
    fun `out of order and duplicate systematic frames do not break decoder`() {
        val blocks = List(5) { index -> ByteArray(16) { (index + it).toByte() } }
        val metadata = metadata(blocks)
        val decoder = FountainDecoder(metadata)
        blocks.indices.reversed().forEach { index ->
            val frame = systematic(metadata.transferId, index, blocks[index], index.toLong())
            decoder.accept(frame)
            decoder.accept(frame)
        }

        assertTrue(decoder.isComplete())
        blocks.indices.forEach { index -> assertArrayEquals(blocks[index], decoder.block(index)) }
    }

    private fun metadata(blocks: List<ByteArray>) = FileMetadata(
        transferId = TransferId(ByteArray(16) { (it + 3).toByte() }),
        fileName = "fountain.bin",
        mimeType = "application/octet-stream",
        sizeBytes = (blocks.size * blocks.first().size).toLong(),
        sha256 = MessageDigest.getInstance("SHA-256").digest(blocks.fold(ByteArray(0)) { all, block -> all + block }),
        blockSize = blocks.first().size,
        sourceBlockCount = blocks.size,
    )

    private fun systematic(id: TransferId, index: Int, payload: ByteArray, sequence: Long) =
        FrameEnvelope(1, 0, id, FrameKind.SYSTEMATIC_DATA, index.toLong(), 1, sequence, payload, 0u)

    private fun repair(id: TransferId, seed: Long, payload: ByteArray, sequence: Long) =
        FrameEnvelope(1, 0, id, FrameKind.REPAIR_DATA, seed, 1, sequence, payload, 0u)
}
