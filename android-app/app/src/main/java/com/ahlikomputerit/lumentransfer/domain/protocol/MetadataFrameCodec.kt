package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import java.io.ByteArrayOutputStream
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
}
