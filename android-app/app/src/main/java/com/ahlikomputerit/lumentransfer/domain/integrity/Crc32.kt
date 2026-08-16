package com.ahlikomputerit.lumentransfer.domain.integrity

import java.util.zip.CRC32

object Crc32 {
    fun compute(bytes: ByteArray): UInt {
        val crc = CRC32()
        crc.update(bytes)
        return crc.value.toUInt()
    }
}
