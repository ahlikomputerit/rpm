package com.ahlikomputerit.lumentransfer.domain.protocol

object ProtocolConstants {
    const val MAGIC_HIGH: Byte = 0x4C
    const val MAGIC_LOW: Byte = 0x54
    const val VERSION = 1
    const val HEADER_SIZE = 35
    const val CRC_SIZE = 4
    const val MAX_PAYLOAD_BYTES = 1024
    const val MAX_FILE_BYTES = 10L * 1024L * 1024L
}
