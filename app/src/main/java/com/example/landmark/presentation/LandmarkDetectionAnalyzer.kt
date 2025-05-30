package com.example.landmark.presentation

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.landmark.domain.Detection
import com.example.landmark.domain.LandmarkDetector

class LandmarkDetectionAnalyzer(
    private val detector: LandmarkDetector, private val onResults: (List<Detection>) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0

    override fun analyze(image: ImageProxy) {
        if (frameSkipCounter % 60 == 0) {
            val rotationDegrees = image.imageInfo.rotationDegrees
            val bitmap = image.toBitmap().centerCrop(320, 320)
            val results = detector.detect(bitmap, rotationDegrees)
            onResults(results)
        }
        frameSkipCounter++
        image.close()
    }
}