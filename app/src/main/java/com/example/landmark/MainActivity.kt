package com.example.landmark

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
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
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, PERMISSIONS, 0
            )
        }

        enableEdgeToEdge()
        setContent {
            LandmarkApp(applicationContext)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
        )
    }
}

@Composable
fun LandmarkApp(context: Context) {
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    var currentMode by rememberSaveable { mutableStateOf(Mode.DETECTION) }

    var detections by remember { mutableStateOf(emptyList<Detection>()) }
    var classifications by remember { mutableStateOf(emptyList<Classification>()) }

    val analyzer = remember(currentMode) {
        when (currentMode) {
            Mode.DETECTION -> LandmarkDetectionAnalyzer(
                detector = TfLiteLandmarkDetector(context), onResults = { detections = it })

            Mode.CLASSIFICATION -> LandmarkClassificationAnalyzer(
                classifier = TfLiteLandmarkClassifier(context),
                onResults = { classifications = it })
        }
    }

    LaunchedEffect(analyzer) {
        controller.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context), analyzer
        )
    }

    LandmarkTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            CameraPreview(
                controller = controller, modifier = Modifier.fillMaxSize()
            )

            if (currentMode == Mode.DETECTION) {
                DetectionOverlay(
                    detections = detections, modifier = Modifier.fillMaxSize()
                )
            }

            if (currentMode == Mode.CLASSIFICATION) {
                ClassificationOverlay(
                    classifications = classifications, modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ModeSwitchButton(
                    currentMode = currentMode, onModeChange = { newMode -> currentMode = newMode })
            }
        }
    }
}

enum class Mode {
    DETECTION, CLASSIFICATION
}

@Composable
private fun DetectionOverlay(
    detections: List<Detection>, modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val scaleFactor = min(
            canvasWidth / 320f, canvasHeight / 320f
        )
        val offsetX = (canvasWidth - 320 * scaleFactor) / 2
        val offsetY = (canvasHeight - 320 * scaleFactor) / 2

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.RED
            textSize = 40f
            isAntiAlias = true
        }

        detections.forEach { detection ->
            val location = detection.location

            val left = offsetX + location.left * scaleFactor
            val top = offsetY + location.top * scaleFactor
            val right = offsetX + location.right * scaleFactor
            val bottom = offsetY + location.bottom * scaleFactor

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 3f)
            )

            drawIntoCanvas { canvas ->
                val text = "${detection.label} (${"%.2f".format(detection.score)})"

                canvas.nativeCanvas.drawText(
                    text, left, top - 10f, textPaint
                )
            }
        }
    }
}

@Composable
private fun ClassificationOverlay(
    classifications: List<Classification>, modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 64.dp)
    ) {

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GREEN
            textSize = 60f
            isAntiAlias = true
        }

        val topClassification = classifications.maxByOrNull { it.score }

        topClassification?.let { classification ->
            drawIntoCanvas { canvas ->
                val text = "${classification.name} (${"%.2f".format(classification.score)})"
                val xPos = (size.width - textPaint.measureText(text)) / 2

                canvas.nativeCanvas.drawText(
                    text, xPos, 100f, textPaint
                )
            }
        }
    }
}

@Composable
fun ModeSwitchButton(
    currentMode: Mode, onModeChange: (Mode) -> Unit, modifier: Modifier = Modifier
) {
    val nextMode = when (currentMode) {
        Mode.DETECTION -> Mode.CLASSIFICATION
        Mode.CLASSIFICATION -> Mode.DETECTION
    }

    val buttonText = when (currentMode) {
        Mode.DETECTION -> "Switch to Classification"
        Mode.CLASSIFICATION -> "Switch to Detection"
    }

    Button(
        onClick = { onModeChange(nextMode) }, modifier = modifier
    ) {
        Text(buttonText)
    }
}