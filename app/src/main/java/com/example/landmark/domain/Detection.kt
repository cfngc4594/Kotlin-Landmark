package com.example.landmark.domain

import android.graphics.RectF

data class Detection(
    val label: String, val score: Float, val location: RectF
)
