package com.ahlikomputerit.lumentransfer.data.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.ReaderException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
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
                // CameraX RGBA_8888 exposes bytes as A, R, G, B in the first plane.
                val alpha = rgba[offset].toInt() and 0xFF
                val red = rgba[offset + 1].toInt() and 0xFF
                val green = rgba[offset + 2].toInt() and 0xFF
                val blue = rgba[offset + 3].toInt() and 0xFF
                argb[y * width + x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }

        val normalHints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.TRY_HARDER, true)
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
        }
        val pureHints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.TRY_HARDER, true)
            put(DecodeHintType.PURE_BARCODE, true)
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
        }

        // A live camera frame normally contains preview background and perspective, so use the
        // detector first. A pure-barcode fallback preserves compatibility with cropped fixtures.
        val text = decodeText(argb, width, height, normalHints)
            ?: decodeText(argb, width, height, pureHints)
            ?: return null

        return try {
            java.util.Base64.getUrlDecoder().decode(text)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun decodeText(
        argb: IntArray,
        width: Int,
        height: Int,
        hints: Map<DecodeHintType, Any>,
    ): String? {
        return try {
            val source = RGBLuminanceSource(width, height, argb)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            QRCodeReader().decode(bitmap, hints).text
        } catch (_: ReaderException) {
            null
        }
    }
}
