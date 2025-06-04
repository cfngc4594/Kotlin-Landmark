package com.example.landmark.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.view.Surface
import com.example.landmark.domain.Detection
import com.example.landmark.domain.LandmarkDetector
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class TfLiteLandmarkDetector(
    private val context: Context,
    private val threshold: Float = 0.5f,
    private val maxResults: Int = 10
) : LandmarkDetector {
    private var detector: ObjectDetector? = null

    private fun setupDetector() {
        val baseOptions = BaseOptions.builder().setNumThreads(2).build()
        val options = ObjectDetector.ObjectDetectorOptions.builder().setBaseOptions(baseOptions)
            .setMaxResults(maxResults).setScoreThreshold(threshold).build()

        try {
            detector = ObjectDetector.createFromFileAndOptions(
                context, "efficientdet.tflite", options
            )
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    override fun detect(
        bitmap: Bitmap, rotation: Int
    ): List<Detection> {
        if (detector == null) {
            setupDetector()
        }

        val imageProcessor = ImageProcessor.Builder().build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val imageProcessingOptions =
            ImageProcessingOptions.builder().setOrientation(getOrientationFromRotation(rotation))
                .build()

        val results = detector?.detect(tensorImage, imageProcessingOptions)

        return results?.map { detection ->
            Detection(
                label = detection.categories.first().label,
                score = detection.categories.first().score,
                location = RectF(
                    detection.boundingBox
                )
            )
        } ?: emptyList()
    }

    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            0 -> ImageProcessingOptions.Orientation.TOP_LEFT

            90 -> {
                println("rotation is 90")
                ImageProcessingOptions.Orientation.TOP_LEFT
            }

            180 -> ImageProcessingOptions.Orientation.LEFT_BOTTOM

            else -> {
                println("rotation is 270")
                ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            }
        }
    }
}