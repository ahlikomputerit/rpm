package com.ahlikomputerit.lumentransfer.data.qr

/**
 * QR boundary used by sender and receiver. The domain only sees a matrix,
 * while ZXing and future CameraX adapters remain behind this interface.
 */
interface QrEncoder {
    fun encode(payload: ByteArray): QrMatrix
}

interface QrDecoder {
    fun decode(matrix: QrMatrix): ByteArray?
}

interface QrImageDecoder {
    fun decodeLuma(
        luma: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        rotationDegrees: Int,
    ): ByteArray? = null

    fun decodeRgba(
        rgba: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        rotationDegrees: Int,
    ): ByteArray?
}

data class QrMatrix(
    val modules: Int,
    val darkModules: BooleanArray,
) {
    init {
        require(modules > 0) { "QR matrix size must be positive" }
        require(darkModules.size == modules * modules) { "QR matrix must be square" }
    }

    fun isDark(x: Int, y: Int): Boolean = darkModules[y * modules + x]
}
