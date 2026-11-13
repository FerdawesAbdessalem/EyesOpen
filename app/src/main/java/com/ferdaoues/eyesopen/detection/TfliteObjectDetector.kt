package com.ferdaoues.eyesopen.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.ferdaoues.eyesopen.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

/**
 * SSD MobileNet v1 (COCO, 90 classes) running on the device.
 *
 * Model output per detection i: box = locations[4i .. 4i+3] as (top, left, bottom, right),
 * class index = classes[i], confidence = scores[i].
 */
class TfliteObjectDetector(context: Context) : ObjectDetector {

    private val model = SsdMobilenetV11Metadata1.newInstance(context)
    private val labels: List<String> = loadLabels(context)
    private val processor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    override fun detect(bitmap: Bitmap, minScore: Float): List<Detection> {
        val image = processor.process(TensorImage.fromBitmap(bitmap))
        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val count = outputs.numberOfDetectionsAsTensorBuffer.floatArray.firstOrNull()?.toInt() ?: scores.size

        return (0 until minOf(count, scores.size))
            .asSequence()
            .filter { scores[it] >= minScore }
            .mapNotNull { i ->
                val label = labels.getOrNull(classes[i].toInt())
                if (label.isNullOrBlank() || label == UNKNOWN) return@mapNotNull null
                val b = 4 * i
                Detection(
                    label = label,
                    score = scores[i],
                    box = RectF(
                        locations[b + 1].coerceIn(0f, 1f),
                        locations[b].coerceIn(0f, 1f),
                        locations[b + 3].coerceIn(0f, 1f),
                        locations[b + 2].coerceIn(0f, 1f),
                    ),
                )
            }
            .sortedByDescending { it.score }
            .toList()
    }

    override fun close() = model.close()

    private companion object {
        const val INPUT_SIZE = 300
        const val UNKNOWN = "???"

        /** French labels when the device is in French, the original English ones otherwise. */
        fun loadLabels(context: Context): List<String> {
            val french = context.resources.configuration.locales[0].language == "fr"
            return FileUtil.loadLabels(context, if (french) "labels_fr.txt" else "labels.txt")
        }
    }
}
