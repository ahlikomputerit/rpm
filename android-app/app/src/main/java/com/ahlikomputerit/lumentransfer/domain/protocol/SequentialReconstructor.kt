package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.integrity.Sha256Hasher
import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import java.io.File
import java.io.RandomAccessFile
import java.util.BitSet

class SequentialReconstructor(private val filesDir: File) : AutoCloseable {
    private var metadata: FileMetadata? = null
    private var tempFile: File? = null
    private var randomAccess: RandomAccessFile? = null
    private var recovered = BitSet()
    private var verified = false

    fun acceptMetadata(next: FileMetadata) {
        require(next.sizeBytes <= ProtocolConstants.MAX_FILE_BYTES) { "File exceeds reconstruction limit" }
        cleanup()
        metadata = next
        tempFile = File.createTempFile("lumen-transfer-", ".part", filesDir)
        randomAccess = RandomAccessFile(tempFile, "rw").also { it.setLength(next.sizeBytes) }
        recovered = BitSet(next.sourceBlockCount)
        verified = next.sourceBlockCount == 0
    }

    fun acceptData(frame: FrameEnvelope): ReconstructionProgress {
        val current = metadata ?: error("Metadata frame is required before data")
        require(frame.kind == FrameKind.SYSTEMATIC_DATA) { "Sequential reconstructor accepts data frames only" }
        require(frame.transferId == current.transferId) { "Transfer ID mismatch" }
        val index = frame.sequence - 1
        require(index in 0 until current.sourceBlockCount) { "Data frame sequence is out of range" }
        if (recovered[index.toInt()]) return progress()

        val expectedSize = minOf(
            current.blockSize.toLong(),
            current.sizeBytes - index * current.blockSize.toLong(),
        ).toInt()
        require(frame.payload.size == expectedSize) { "Data frame payload size is invalid" }
        randomAccess?.seek(index * current.blockSize.toLong())
        randomAccess?.write(frame.payload)
        recovered.set(index.toInt())
        return progress()
    }

    fun isComplete(): Boolean = metadata?.let { recovered.cardinality() == it.sourceBlockCount } == true

    fun verify(): Boolean {
        val current = metadata ?: return false
        if (!isComplete()) return false
        val file = tempFile ?: return false
        val checksum = file.inputStream().use(Sha256Hasher::compute)
        verified = checksum.contentEquals(current.sha256)
        if (!verified) cleanup()
        return verified
    }

    fun verifiedFile(): File? = tempFile?.takeIf { verified && it.exists() }

    fun progress(): ReconstructionProgress = ReconstructionProgress(
        recoveredBlocks = recovered.cardinality(),
        totalBlocks = metadata?.sourceBlockCount ?: 0,
        fileSizeBytes = metadata?.sizeBytes ?: 0L,
    )

    fun currentMetadata(): FileMetadata? = metadata

    fun cleanup() {
        randomAccess?.close()
        randomAccess = null
        tempFile?.delete()
        tempFile = null
        metadata = null
        recovered.clear()
        verified = false
    }

    override fun close() {
        cleanup()
    }
}

data class ReconstructionProgress(
    val recoveredBlocks: Int,
    val totalBlocks: Int,
    val fileSizeBytes: Long,
    val equationCount: Int = recoveredBlocks,
) {
    val isComplete: Boolean get() = recoveredBlocks == totalBlocks
}
