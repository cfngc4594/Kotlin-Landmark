package com.example.landmark.data

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface
import com.example.landmark.domain.Classification
import com.example.landmark.domain.LandmarkClassifier
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class TfLiteLandmarkClassifier(
    private val context: Context,
    private val threshold: Float = 0.5f,
    private val maxResults: Int = 1
) : LandmarkClassifier {
    private var classifier: ImageClassifier? = null

    private fun setupClassifier() {
        val baseOptions = BaseOptions.builder().setNumThreads(2).build()
        val options = ImageClassifier.ImageClassifierOptions.builder().setBaseOptions(baseOptions)
            .setMaxResults(maxResults).setScoreThreshold(threshold).build()

        try {
            classifier = ImageClassifier.createFromFileAndOptions(
                context, "landmarks.tflite", options
            )
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    override fun classify(bitmap: Bitmap, rotation: Int): List<Classification> {
        if (classifier == null) {
            setupClassifier()
        }

        val imageProcessor = ImageProcessor.Builder().build()
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val imageProcessingOptions =
            ImageProcessingOptions.builder().setOrientation(getOrientationFromRotation(rotation))
                .build()

        val results = classifier?.classify(tensorImage, imageProcessingOptions)

        return results?.flatMap { classifications ->
            classifications.categories.map { category ->
                Classification(
                    name = category.displayName, score = category.score
                )
            }.distinctBy { it.name }
        } ?: emptyList()
    }


    private fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when (rotation) {
            0 -> ImageProcessingOptions.Orientation.TOP_LEFT

            90 -> {
                println("TODO: rotation is 90")
                ImageProcessingOptions.Orientation.TOP_LEFT
            }

            180 -> ImageProcessingOptions.Orientation.LEFT_BOTTOM

            else -> {
                println("TODO: rotation is 270")
                ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            }
        }
    }
}