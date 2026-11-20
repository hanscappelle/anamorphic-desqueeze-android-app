package be.hcpl.android.photofilters

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.media3.effect.CanvasOverlay
import kotlin.math.roundToInt

internal data class LetterboxBars(val top: Int, val bottom: Int)

internal class InvalidLetterboxRatioException : IllegalArgumentException()

internal fun calculateLetterboxBars(width: Int, height: Int, targetRatio: Float): LetterboxBars {
    if (width <= 0 || height <= 0 || !targetRatio.isFinite() || targetRatio <= 0f ||
        targetRatio < width.toFloat() / height
    ) throw InvalidLetterboxRatioException()
    val visibleHeight = (width.toDouble() / targetRatio).roundToInt()
    if (visibleHeight < 1) throw InvalidLetterboxRatioException()
    val top = (height - visibleHeight) / 2
    return LetterboxBars(top, height - visibleHeight - top)
}

internal fun drawLetterboxBars(canvas: Canvas, targetRatio: Float) {
    val bars = calculateLetterboxBars(canvas.width, canvas.height, targetRatio)
    val paint = Paint().apply { color = Color.BLACK }
    canvas.drawRect(0f, 0f, canvas.width.toFloat(), bars.top.toFloat(), paint)
    canvas.drawRect(
        0f, (canvas.height - bars.bottom).toFloat(),
        canvas.width.toFloat(), canvas.height.toFloat(), paint,
    )
}

internal class LetterboxOverlay(private val targetRatio: Float) : CanvasOverlay(true) {
    override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
        drawLetterboxBars(canvas, targetRatio)
    }
}
