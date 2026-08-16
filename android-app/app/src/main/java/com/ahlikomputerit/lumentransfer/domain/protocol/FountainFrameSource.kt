package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import java.io.ByteArrayOutputStream
import java.io.InputStream

class FountainFrameSource(
    private val metadata: FileMetadata,
    inputStreamFactory: () -> InputStream,
) : AutoCloseable {
    private val blocks: List<ByteArray> = loadBlocks(inputStreamFactory)
    private val repairBudget = FountainCodec.repairFrameBudget(metadata.sourceBlockCount)
    private var metadataSent = false
    private var systematicIndex = 0
    private var repairIndex = 0
    private var endSent = false
    private var sequence = 0L

    fun nextEnvelope(): FrameEnvelope {
        if (!metadataSent) {
            metadataSent = true
            return envelope(FrameKind.META, seed = 0, degree = 0, payload = MetadataFrameCodec.encode(metadata))
        }
        if (systematicIndex < blocks.size) {
            val index = systematicIndex++
            return envelope(
                kind = FrameKind.SYSTEMATIC_DATA,
                seed = index.toLong(),
                degree = 1,
                payload = blocks[index].copyOf(),
            )
        }
        if (repairIndex < repairBudget) {
            val seed = repairIndex++.toLong()
            val degree = FountainCodec.degree(seed, blocks.size)
            val indices = FountainCodec.chooseIndices(seed, blocks.size, degree)
            val payload = ByteArray(metadata.blockSize)
            indices.forEach { FountainCodec.xorInto(payload, blocks[it]) }
            return envelope(FrameKind.REPAIR_DATA, seed, degree, payload)
        }
        if (!endSent) {
            endSent = true
            return envelope(FrameKind.END, seed = 0, degree = 0, payload = ByteArray(0))
        }
        return envelope(FrameKind.END, seed = 0, degree = 0, payload = ByteArray(0))
    }

    fun reset() {
        metadataSent = false
        systematicIndex = 0
        repairIndex = 0
        endSent = false
        sequence = 0L
    }

    override fun close() = Unit

    private fun envelope(kind: FrameKind, seed: Long, degree: Int, payload: ByteArray) =
        FrameEnvelope(
            version = ProtocolConstants.VERSION,
            flags = 0,
            transferId = metadata.transferId,
            kind = kind,
            seed = seed,
            degree = degree,
            sequence = sequence++,
            payload = payload,
            frameCrc32 = 0u,
        )

    private fun loadBlocks(factory: () -> InputStream): List<ByteArray> {
        require(metadata.sizeBytes <= ProtocolConstants.MAX_FILE_BYTES) { "File exceeds fountain memory limit" }
        val output = ByteArrayOutputStream(metadata.sizeBytes.toInt())
        factory().use { input ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) {
                    require(output.size() + read <= ProtocolConstants.MAX_FILE_BYTES) { "File exceeds fountain memory limit" }
                    output.write(buffer, 0, read)
                }
            }
        }
        val bytes = output.toByteArray()
        require(bytes.size.toLong() == metadata.sizeBytes) { "Source size changed during fountain preparation" }
        return (0 until metadata.sourceBlockCount).map { index ->
            val block = ByteArray(metadata.blockSize)
            val start = index * metadata.blockSize
            val length = minOf(metadata.blockSize, bytes.size - start)
            if (length > 0) bytes.copyInto(block, 0, start, start + length)
            block
        }
    }
}
