package com.ahlikomputerit.lumentransfer.presentation.send

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ahlikomputerit.lumentransfer.data.qr.QrMatrix
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun QrMatrixCanvas(
    matrix: QrMatrix,
    modifier: Modifier = Modifier,
    contentDescription: String = "QR frame preview",
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.White)
            .padding(12.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val moduleSize = size.minDimension / matrix.modules
        drawRect(Color.White, topLeft = Offset.Zero, size = size)
        for (y in 0 until matrix.modules) {
            for (x in 0 until matrix.modules) {
                if (matrix.isDark(x, y)) {
                    val left = floor(x * moduleSize)
                    val top = floor(y * moduleSize)
                    val right = ceil((x + 1) * moduleSize)
                    val bottom = ceil((y + 1) * moduleSize)
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                    )
                }
            }
        }
    }
}
