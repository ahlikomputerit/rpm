package com.ahlikomputerit.lumentransfer.data.qr

/**
 * Dependency boundary for a JVM/Kotlin-compatible QR implementation.
 * The production codec is intentionally deferred until the library spike
 * verifies payload capacity, license, and physical-device decode rate.
 */
interface QrEncoder {
    fun encode(payload: ByteArray): QrMatrix
}

interface QrDecoder {
    fun decode(frame: ByteArray): ByteArray?
}

data class QrMatrix(val modules: Int, val darkModules: BooleanArray)

class PlaceholderQrEncoder : QrEncoder {
    override fun encode(payload: ByteArray): QrMatrix =
        QrMatrix(modules = 0, darkModules = BooleanArray(0))
}

class PlaceholderQrDecoder : QrDecoder {
    override fun decode(frame: ByteArray): ByteArray? = null
}
