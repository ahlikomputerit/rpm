package com.ahlikomputerit.lumentransfer.domain.model

import java.util.Arrays

data class TransferId(val bytes: ByteArray) {
    init {
        require(bytes.size == LENGTH) { "Transfer ID must be exactly $LENGTH bytes" }
    }

    override fun equals(other: Any?): Boolean = other is TransferId && bytes.contentEquals(other.bytes)
    override fun hashCode(): Int = Arrays.hashCode(bytes)
    override fun toString(): String = bytes.joinToString("") { "%02x".format(it) }

    companion object {
        const val LENGTH = 16
    }
}

data class FileMetadata(
    val transferId: TransferId,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: ByteArray,
    val blockSize: Int = 1024,
    val sourceBlockCount: Int = 0,
) {
    init {
        require(fileName.isNotBlank()) { "File name must not be blank" }
        require(sizeBytes >= 0) { "File size must not be negative" }
        require(sha256.size == 32) { "SHA-256 must be 32 bytes" }
        require(blockSize > 0) { "Block size must be positive" }
        require(sourceBlockCount >= 0) { "Source block count must not be negative" }
    }
}

enum class FrameKind(val wireValue: Int) {
    META(1),
    SYSTEMATIC_DATA(2),
    REPAIR_DATA(3),
    END(4),
    ;

    companion object {
        fun fromWireValue(value: Int): FrameKind = entries.firstOrNull { it.wireValue == value }
            ?: throw IllegalArgumentException("Unsupported frame kind: $value")
    }
}

data class FrameEnvelope(
    val version: Int,
    val flags: Int,
    val transferId: TransferId,
    val kind: FrameKind,
    val seed: Long,
    val degree: Int,
    val sequence: Long,
    val payload: ByteArray,
    val frameCrc32: UInt,
) {
    init {
        require(version in 0..255) { "Version must fit one unsigned byte" }
        require(flags in 0..255) { "Flags must fit one unsigned byte" }
        require(seed >= 0) { "Seed must not be negative" }
        require(degree >= 0) { "Degree must not be negative" }
        require(sequence >= 0) { "Sequence must not be negative" }
        require(payload.size <= 0xFFFF) { "Payload is too large for the envelope" }
    }
}

enum class TransferMode {
    SEND,
    RECEIVE,
}

enum class TransferError {
    FILE_UNREADABLE,
    FILE_TOO_LARGE,
    CAMERA_PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    UNSUPPORTED_PROTOCOL,
    FRAME_CORRUPT,
    TRANSFER_ID_MISMATCH,
    DECODER_STALLED,
    INTEGRITY_MISMATCH,
    STORAGE_WRITE_FAILED,
    SESSION_CANCELLED,
}
