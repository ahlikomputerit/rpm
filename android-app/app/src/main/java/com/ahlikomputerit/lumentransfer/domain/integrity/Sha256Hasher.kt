package com.ahlikomputerit.lumentransfer.domain.integrity

import java.io.InputStream
import java.security.MessageDigest

object Sha256Hasher {
    fun compute(input: InputStream, bufferSize: Int = 16 * 1024): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(bufferSize)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
        return digest.digest()
    }
}
