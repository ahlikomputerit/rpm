package com.ahlikomputerit.lumentransfer.data.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.ReaderException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
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
                // CameraX RGBA_8888 exposes bytes as R, G, B, A in the first plane.
                val red = rgba[offset].toInt() and 0xFF
                val green = rgba[offset + 1].toInt() and 0xFF
                val blue = rgba[offset + 2].toInt() and 0xFF
                val alpha = rgba[offset + 3].toInt() and 0xFF
                argb[y * width + x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }

        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.TRY_HARDER, true)
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
        }
        val pureHints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.TRY_HARDER, true)
            put(DecodeHintType.PURE_BARCODE, true)
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
        }

        // Try the complete preview first, then center crops. A phone screen is normally centered
        // in the receiver preview, and a crop gives the detector more pixels per QR module.
        val candidates = listOf(
            Crop(0, 0, width, height),
            centeredCrop(width, height, 0.80f),
            centeredCrop(width, height, 0.60f),
        )
        for (crop in candidates) {
            for (global in listOf(false, true)) {
                decodeText(argb, width, height, crop, hints, global)?.let { return decodeBase64(it) }
            }
        }
        // Preserve compatibility with a QR bitmap that is already cropped to barcode-only input.
        decodeText(argb, width, height, Crop(0, 0, width, height), pureHints, global = false)?.let {
            return decodeBase64(it)
        }
        return null
    }

    private fun decodeText(
        argb: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        crop: Crop,
        hints: Map<DecodeHintType, Any>,
        global: Boolean,
    ): String? {
        return try {
            val fullSource = RGBLuminanceSource(sourceWidth, sourceHeight, argb)
            val source = if (crop.x == 0 && crop.y == 0 && crop.width == sourceWidth && crop.height == sourceHeight) {
                fullSource
            } else {
                fullSource.crop(crop.x, crop.y, crop.width, crop.height)
            }
            val binarizer = if (global) GlobalHistogramBinarizer(source) else HybridBinarizer(source)
            QRCodeReader().decode(BinaryBitmap(binarizer), hints).text
        } catch (_: ReaderException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun decodeBase64(text: String): ByteArray? = try {
        java.util.Base64.getUrlDecoder().decode(text)
    } catch (_: IllegalArgumentException) {
        null
    }

    private data class Crop(val x: Int, val y: Int, val width: Int, val height: Int)

    private fun centeredCrop(width: Int, height: Int, fraction: Float): Crop {
        val cropWidth = (width * fraction).toInt().coerceAtLeast(1)
        val cropHeight = (height * fraction).toInt().coerceAtLeast(1)
        return Crop((width - cropWidth) / 2, (height - cropHeight) / 2, cropWidth, cropHeight)
    }
}
