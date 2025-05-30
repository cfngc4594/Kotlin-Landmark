package com.example.landmark

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.landmark.data.TfLiteLandmarkClassifier
import com.example.landmark.data.TfLiteLandmarkDetector
import com.example.landmark.domain.Classification
import com.example.landmark.domain.Detection
import com.example.landmark.presentation.CameraPreview
import com.example.landmark.presentation.LandmarkClassificationAnalyzer
import com.example.landmark.presentation.LandmarkDetectionAnalyzer
import com.example.landmark.ui.theme.LandmarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }
        setContent {
            LandmarkTheme {
                var detections by remember {
                    mutableStateOf(emptyList<Detection>())
                }
                println(detections)
                val detectionAnalyzer = remember {
                    LandmarkDetectionAnalyzer(
                        detector = TfLiteLandmarkDetector(
                            context = applicationContext
                        ), onResults = {
                            detections = it
                        })
                }
                val controller = remember {
                    LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                        setImageAnalysisAnalyzer(
                            ContextCompat.getMainExecutor(applicationContext), detectionAnalyzer
                        )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CameraPreview(controller, Modifier.fillMaxSize())
                    DetectionOverlay(detections = detections)
                }
            }
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun DetectionOverlay(detections: List<Detection>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // 获取Canvas的实际尺寸
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 假设模型输入是320x320的中心裁剪
        val modelInputSize = 320f
        val scaleX = canvasWidth / modelInputSize
        val scaleY = canvasHeight / modelInputSize

        detections.forEach { detection ->
            val rect = detection.location

            val left = rect.left * scaleX
            val top = rect.top * scaleY
            val right = rect.right * scaleX
            val bottom = rect.bottom * scaleY

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 3f)
            )

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${detection.label} (${detection.score})",
                    left,
                    top - 10,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.RED
                        textSize = 40f
                    }
                )
            }
        }
    }
}