package com.ahlikomputerit.lumentransfer.data.qr

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Base64
import java.util.EnumMap

class ZxingQrEncoder(
    private val errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
    private val margin: Int = 2,
) : QrEncoder {
    override fun encode(payload: ByteArray): QrMatrix {
        require(payload.isNotEmpty()) { "QR payload must not be empty" }
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.ERROR_CORRECTION, errorCorrection)
            put(EncodeHintType.MARGIN, margin)
            put(EncodeHintType.CHARACTER_SET, Charsets.US_ASCII.name())
        }
        val matrix = QRCodeWriter().encode(encoded, com.google.zxing.BarcodeFormat.QR_CODE, 1, 1, hints)
        return matrix.toQrMatrix()
    }
}

class ZxingQrDecoder : QrDecoder {
    override fun decode(matrix: QrMatrix): ByteArray? {
        return try {
            val bitMatrix = matrix.toBitMatrix()
            val source = BitMatrixLuminanceSource(bitMatrix)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
                put(DecodeHintType.PURE_BARCODE, true)
                put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
            }
            val result = QRCodeReader().decode(bitmap, hints)
            Base64.getUrlDecoder().decode(result.text)
        } catch (_: ReaderException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}

private fun BitMatrix.toQrMatrix(): QrMatrix {
    val size = width
    require(width == height) { "ZXing QR matrix must be square" }
    return QrMatrix(
        modules = size,
        darkModules = BooleanArray(size * size) { index ->
            val x = index % size
            val y = index / size
            get(x, y)
        },
    )
}

private fun QrMatrix.toBitMatrix(): BitMatrix {
    val matrix = BitMatrix(modules, modules)
    for (y in 0 until modules) {
        for (x in 0 until modules) {
            if (isDark(x, y)) matrix.set(x, y)
        }
    }
    return matrix
}

private class BitMatrixLuminanceSource(private val matrix: BitMatrix) : LuminanceSource(matrix.width, matrix.height) {
    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val output = if (row != null && row.size >= width) row else ByteArray(width)
        for (x in 0 until width) output[x] = if (matrix.get(x, y)) 0 else 0xFF.toByte()
        return output
    }

    override fun getMatrix(): ByteArray {
        val output = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                output[y * width + x] = if (matrix.get(x, y)) 0 else 0xFF.toByte()
            }
        }
        return output
    }

    override fun isCropSupported(): Boolean = false
    override fun isRotateSupported(): Boolean = false
}

fun QrMatrix.toBitmap(scale: Int = 8): Bitmap {
    require(scale > 0) { "Scale must be positive" }
    val bitmap = Bitmap.createBitmap(modules * scale, modules * scale, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(bitmap.width * bitmap.height)
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val dark = isDark(x / scale, y / scale)
            pixels[y * bitmap.width + x] = if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return bitmap
}
