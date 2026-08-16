package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.integrity.Sha256Hasher
import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import java.io.File
import java.io.RandomAccessFile

class FountainReconstructor(private val filesDir: File) : AutoCloseable {
    private var metadata: FileMetadata? = null
    private var decoder: FountainDecoder? = null
    private var tempFile: File? = null
    private var randomAccess: RandomAccessFile? = null
    private var persisted = false
    private var verified = false

    fun acceptMetadata(next: FileMetadata) {
        require(next.sizeBytes <= ProtocolConstants.MAX_FILE_BYTES) { "File exceeds reconstruction limit" }
        cleanup()
        metadata = next
        decoder = FountainDecoder(next)
        tempFile = File.createTempFile("lumen-transfer-", ".part", filesDir)
        randomAccess = RandomAccessFile(tempFile, "rw").also { it.setLength(next.sizeBytes) }
        persisted = false
        verified = false
    }

    fun acceptData(frame: FrameEnvelope): ReconstructionProgress {
        val current = metadata ?: error("Metadata frame is required before data")
        require(frame.kind == FrameKind.SYSTEMATIC_DATA || frame.kind == FrameKind.REPAIR_DATA) {
            "Fountain reconstructor accepts data or repair frames only"
        }
        val progress = decoder?.accept(frame) ?: error("Fountain decoder is unavailable")
        if (progress.isComplete && !persisted) {
            persistRecoveredBlocks(current)
            persisted = true
        }
        return ReconstructionProgress(
            recoveredBlocks = progress.recoveredBlocks,
            totalBlocks = progress.totalBlocks,
            fileSizeBytes = current.sizeBytes,
            equationCount = progress.equationCount,
        )
    }

    fun isComplete(): Boolean = metadata?.let { it.sourceBlockCount == 0 || decoder?.isComplete() == true } == true

    fun verify(): Boolean {
        val current = metadata ?: return false
        if (!isComplete()) return false
        if (current.sourceBlockCount > 0 && !persisted) return false
        val file = tempFile ?: return false
        val checksum = file.inputStream().use(Sha256Hasher::compute)
        verified = checksum.contentEquals(current.sha256)
        if (!verified) cleanup()
        return verified
    }

    fun verifiedFile(): File? = tempFile?.takeIf { verified && it.exists() }

    fun progress(): ReconstructionProgress {
        val current = metadata
        val progress = decoder?.progress()
        return ReconstructionProgress(
            recoveredBlocks = progress?.recoveredBlocks ?: if (current?.sourceBlockCount == 0) 0 else 0,
            totalBlocks = current?.sourceBlockCount ?: 0,
            fileSizeBytes = current?.sizeBytes ?: 0L,
            equationCount = progress?.equationCount ?: 0,
        )
    }

    fun currentMetadata(): FileMetadata? = metadata

    fun cleanup() {
        randomAccess?.close()
        randomAccess = null
        tempFile?.delete()
        tempFile = null
        metadata = null
        decoder = null
        persisted = false
        verified = false
    }

    override fun close() {
        cleanup()
    }

    private fun persistRecoveredBlocks(current: FileMetadata) {
        val output = randomAccess ?: error("Temporary output is unavailable")
        for (index in 0 until current.sourceBlockCount) {
            output.seek(index * current.blockSize.toLong())
            output.write(decoder!!.block(index))
        }
    }
}
