package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import java.io.InputStream

class SequentialFrameSource(
    private val metadata: FileMetadata,
    private val inputStreamFactory: () -> InputStream,
) : AutoCloseable {
    private var metadataSent = false
    private var endSent = false
    private var sequence = 0L
    private var input: InputStream? = null

    fun nextEnvelope(): FrameEnvelope {
        if (!metadataSent) {
            metadataSent = true
            return envelope(
                kind = FrameKind.META,
                payload = MetadataFrameCodec.encode(metadata),
                degree = 0,
            )
        }

        if (endSent) {
            return envelope(kind = FrameKind.END, payload = ByteArray(0), degree = 0)
        }

        val stream = input ?: inputStreamFactory().also { input = it }
        val buffer = ByteArray(metadata.blockSize)
        val read = stream.read(buffer)
        if (read < 0) {
            endSent = true
            stream.close()
            input = null
            return envelope(kind = FrameKind.END, payload = ByteArray(0), degree = 0)
        }
        if (read == 0) return nextEnvelope()
        return envelope(
            kind = FrameKind.SYSTEMATIC_DATA,
            payload = buffer.copyOf(read),
            degree = 1,
        )
    }

    fun reset() {
        closeInput()
        metadataSent = false
        endSent = false
        sequence = 0L
    }

    override fun close() {
        closeInput()
    }

    private fun envelope(kind: FrameKind, payload: ByteArray, degree: Int): FrameEnvelope =
        FrameEnvelope(
            version = ProtocolConstants.VERSION,
            flags = 0,
            transferId = metadata.transferId,
            kind = kind,
            seed = sequence,
            degree = degree,
            sequence = sequence++,
            payload = payload,
            frameCrc32 = 0u,
        )

    private fun closeInput() {
        input?.close()
        input = null
    }
}
