package com.ferdaoues.eyesopen.detection

import android.graphics.Bitmap

/**
 * Abstraction over the detection model, so the camera screen does not depend
 * on TensorFlow Lite (another model or ML Kit could be plugged in).
 */
interface ObjectDetector : AutoCloseable {
    /** Detects objects in [bitmap] and returns those above [minScore], best first. */
    fun detect(bitmap: Bitmap, minScore: Float): List<Detection>
}
