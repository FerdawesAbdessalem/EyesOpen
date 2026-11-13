package com.ferdaoues.eyesopen.detection

import android.graphics.RectF

/**
 * One detected object.
 *
 * @param label  translated label, ready to display and to speak
 * @param score  confidence between 0 and 1
 * @param box    bounding box in normalised coordinates (0..1) of the analysed frame
 */
data class Detection(
    val label: String,
    val score: Float,
    val box: RectF,
)

/** Where an object is in the frame, from the user's point of view. */
enum class Position { LEFT, AHEAD, RIGHT }

/** Pure geometry helpers (unit-tested, no Android framework calls). */
object Spatial {
    /** Box covering more than this share of the frame is considered "very close". */
    const val CLOSE_AREA = 0.25f

    fun position(centerX: Float): Position = when {
        centerX < 1f / 3f -> Position.LEFT
        centerX > 2f / 3f -> Position.RIGHT
        else -> Position.AHEAD
    }

    fun isClose(width: Float, height: Float): Boolean = width * height >= CLOSE_AREA
}

fun Detection.position(): Position = Spatial.position(box.centerX())

fun Detection.isClose(): Boolean = Spatial.isClose(box.width(), box.height())
