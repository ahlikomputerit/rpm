package com.ahlikomputerit.lumentransfer.domain.protocol

import com.ahlikomputerit.lumentransfer.domain.integrity.Crc32
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ProtocolException(message: String) : IllegalArgumentException(message)

object FrameSerializer {
    fun serialize(frame: FrameEnvelope): ByteArray {
        require(frame.version == ProtocolConstants.VERSION) {
            "Only protocol version ${ProtocolConstants.VERSION} can be serialized"
        }
        require(frame.seed <= UInt.MAX_VALUE.toLong()) { "Seed exceeds wire range" }
        require(frame.sequence <= UInt.MAX_VALUE.toLong()) { "Sequence exceeds wire range" }
        require(frame.degree <= 0xFFFF) { "Degree exceeds wire range" }
        require(frame.payload.size <= ProtocolConstants.MAX_PAYLOAD_BYTES) { "Payload exceeds QR budget" }

        val withoutCrc = ByteBuffer.allocate(ProtocolConstants.HEADER_SIZE + frame.payload.size)
            .order(ByteOrder.BIG_ENDIAN)
            .apply {
                put(ProtocolConstants.MAGIC_HIGH)
                put(ProtocolConstants.MAGIC_LOW)
                put(frame.version.toByte())
                put(frame.flags.toByte())
                put(frame.transferId.bytes)
                put(frame.kind.wireValue.toByte())
                putInt(frame.seed.toInt())
                putShort(frame.degree.toShort())
                putInt(frame.sequence.toInt())
                putInt(frame.payload.size)
                put(frame.payload)
            }
            .array()

        val crc = Crc32.compute(withoutCrc)
        return ByteBuffer.allocate(withoutCrc.size + ProtocolConstants.CRC_SIZE)
            .order(ByteOrder.BIG_ENDIAN)
            .put(withoutCrc)
            .putInt(crc.toInt())
            .array()
    }

    fun parse(bytes: ByteArray): FrameEnvelope {
        if (bytes.size < ProtocolConstants.HEADER_SIZE + ProtocolConstants.CRC_SIZE) {
            throw ProtocolException("Frame is truncated")
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        if (buffer.get() != ProtocolConstants.MAGIC_HIGH || buffer.get() != ProtocolConstants.MAGIC_LOW) {
            throw ProtocolException("Invalid magic prefix")
        }
        val version = buffer.get().toInt() and 0xFF
        if (version != ProtocolConstants.VERSION) {
            throw ProtocolException("Unsupported protocol version: $version")
        }
        val flags = buffer.get().toInt() and 0xFF
        val transferIdBytes = ByteArray(TransferId.LENGTH)
        buffer.get(transferIdBytes)
        val kind = try {
            FrameKind.fromWireValue(buffer.get().toInt() and 0xFF)
        } catch (error: IllegalArgumentException) {
            throw ProtocolException(error.message ?: "Invalid frame kind")
        }
        val seed = buffer.int.toUInt().toLong()
        val degree = buffer.short.toInt() and 0xFFFF
        val sequence = buffer.int.toUInt().toLong()
        val payloadLength = buffer.int
        if (payloadLength < 0 || payloadLength > ProtocolConstants.MAX_PAYLOAD_BYTES) {
            throw ProtocolException("Invalid payload length: $payloadLength")
        }
        val expectedSize = ProtocolConstants.HEADER_SIZE + payloadLength + ProtocolConstants.CRC_SIZE
        if (bytes.size != expectedSize) {
            throw ProtocolException("Payload length does not match frame size")
        }
        val payload = ByteArray(payloadLength)
        buffer.get(payload)
        val receivedCrc = buffer.int.toUInt()
        val calculatedCrc = Crc32.compute(bytes.copyOfRange(0, bytes.size - ProtocolConstants.CRC_SIZE))
        if (receivedCrc != calculatedCrc) {
            throw ProtocolException("CRC mismatch")
        }

        return FrameEnvelope(
            version = version,
            flags = flags,
            transferId = TransferId(transferIdBytes),
            kind = kind,
            seed = seed,
            degree = degree,
            sequence = sequence,
            payload = payload,
            frameCrc32 = receivedCrc,
        )
    }
}
