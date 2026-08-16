package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object MetadataFrameCodec {
    fun encode(metadata: FileMetadata): ByteArray {
        val name = metadata.fileName.toByteArray(Charsets.UTF_8)
        val mime = metadata.mimeType.toByteArray(Charsets.UTF_8)
        require(name.size <= 0xFFFF) { "File name metadata is too long" }
        require(mime.size <= 0xFFFF) { "MIME metadata is too long" }

        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeShort(name.size)
            data.write(name)
            data.writeShort(mime.size)
            data.write(mime)
            data.writeLong(metadata.sizeBytes)
            data.writeInt(metadata.blockSize)
            data.writeInt(metadata.sourceBlockCount)
            data.write(metadata.sha256)
        }
        return output.toByteArray().also {
            require(it.size <= ProtocolConstants.MAX_PAYLOAD_BYTES) {
                "Metadata exceeds QR payload budget"
            }
        }
    }

    fun decode(transferId: TransferId, payload: ByteArray): FileMetadata {
        require(payload.isNotEmpty()) { "Metadata payload must not be empty" }
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            val nameLength = input.readUnsignedShort()
            require(nameLength <= input.available()) { "Metadata file name is truncated" }
            val nameBytes = ByteArray(nameLength).also(input::readFully)
            val mimeLength = input.readUnsignedShort()
            require(mimeLength <= input.available()) { "Metadata MIME type is truncated" }
            val mimeBytes = ByteArray(mimeLength).also(input::readFully)
            val sizeBytes = input.readLong()
            val blockSize = input.readInt()
            val sourceBlockCount = input.readInt()
            val sha256 = ByteArray(32).also(input::readFully)
            require(input.available() == 0) { "Metadata has trailing bytes" }
            require(sizeBytes >= 0) { "Metadata file size is negative" }
            require(blockSize in 256..ProtocolConstants.MAX_PAYLOAD_BYTES) { "Metadata block size is invalid" }
            val expectedBlocks = if (sizeBytes == 0L) 0 else ((sizeBytes + blockSize - 1) / blockSize).toInt()
            require(sourceBlockCount == expectedBlocks) { "Metadata block count does not match file size" }
            return FileMetadata(
                transferId = transferId,
                fileName = nameBytes.toString(Charsets.UTF_8),
                mimeType = mimeBytes.toString(Charsets.UTF_8),
                sizeBytes = sizeBytes,
                sha256 = sha256,
                blockSize = blockSize,
                sourceBlockCount = sourceBlockCount,
            )
        }
    }
}
