package com.ferdaoues.eyesopen.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.ferdaoues.eyesopen.R
import com.ferdaoues.eyesopen.detection.Detection
import kotlin.math.max

/**
 * Draws the detection boxes and labels on top of the camera preview.
 *
 * Boxes are normalised to the analysed frame; they are mapped to the view with
 * the same "fill center" scaling as the PreviewView, so they line up.
 */
class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var detections: List<Detection> = emptyList()
    private var frameWidth = 1
    private var frameHeight = 1

    private val density = resources.displayMetrics.density
    private val accent = ContextCompat.getColor(context, R.color.accent)

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = accent
    }
    private val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 15f * density
        isFakeBoldText = true
    }
    private val rect = RectF()
    private val chip = RectF()

    fun show(newDetections: List<Detection>, width: Int, height: Int) {
        detections = newDetections
        frameWidth = max(width, 1)
        frameHeight = max(height, 1)
        invalidate()
    }

    fun clear() = show(emptyList(), frameWidth, frameHeight)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (detections.isEmpty()) return

        val scale = max(width.toFloat() / frameWidth, height.toFloat() / frameHeight)
        val dx = (width - frameWidth * scale) / 2f
        val dy = (height - frameHeight * scale) / 2f
        val radius = 10f * density
        val pad = 6f * density

        for (d in detections) {
            rect.set(
                d.box.left * frameWidth * scale + dx,
                d.box.top * frameHeight * scale + dy,
                d.box.right * frameWidth * scale + dx,
                d.box.bottom * frameHeight * scale + dy,
            )
            canvas.drawRoundRect(rect, radius, radius, boxPaint)

            val text = "${d.label} ${(d.score * 100).toInt()} %"
            val textWidth = textPaint.measureText(text)
            val top = (rect.top - textPaint.textSize - 2 * pad).coerceAtLeast(0f)
            chip.set(rect.left, top, rect.left + textWidth + 2 * pad, top + textPaint.textSize + 2 * pad)
            canvas.drawRoundRect(chip, radius, radius, chipPaint)
            canvas.drawText(text, chip.left + pad, chip.bottom - pad - textPaint.descent() / 2, textPaint)
        }
    }
}
