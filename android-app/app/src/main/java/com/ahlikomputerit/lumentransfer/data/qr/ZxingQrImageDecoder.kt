package com.ahlikomputerit.lumentransfer.data.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.qrcode.QRCodeReader
import java.util.EnumMap

class ZxingQrImageDecoder : QrImageDecoder {
    override fun decodeRgba(
        rgba: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        rotationDegrees: Int,
    ): ByteArray? {
        if (width <= 0 || height <= 0 || pixelStride < 4 || rowStride < width * pixelStride) return null
        if (rgba.size < rowStride * height) return null

        val argb = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = y * rowStride + x * pixelStride
                val alpha = rgba[offset].toInt() and 0xFF
                val red = rgba[offset + 1].toInt() and 0xFF
                val green = rgba[offset + 2].toInt() and 0xFF
                val blue = rgba[offset + 3].toInt() and 0xFF
                argb[y * width + x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }

        return try {
            var source: com.google.zxing.LuminanceSource = RGBLuminanceSource(width, height, argb)
            repeat((rotationDegrees.coerceAtLeast(0) / 90) % 4) {
                source = source.rotateCounterClockwise()
            }
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
                put(DecodeHintType.PURE_BARCODE, true)
                put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
            }
            val result = QRCodeReader().decode(bitmap, hints)
            java.util.Base64.getUrlDecoder().decode(result.text)
        } catch (_: ReaderException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
