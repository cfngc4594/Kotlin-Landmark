package com.example.landmark.domain

import android.graphics.Bitmap

interface LandmarkDetector {
    fun detect(bitmap: Bitmap, rotation: Int): List<Detection>
}