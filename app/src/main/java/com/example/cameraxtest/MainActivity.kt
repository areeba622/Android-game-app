package com.example.cameraxtest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: HandOverlayView
    private lateinit var handHelper: HandLandmarkerHelper

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Camera preview
        previewView = PreviewView(this)

        // Hand landmark overlay
        overlayView = HandOverlayView(this)

        // Place overlay above camera preview
        val root = FrameLayout(this)

        root.addView(
            previewView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(
            overlayView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        // Initialize MediaPipe
        handHelper = HandLandmarkerHelper(this)
        handHelper.setup()

        // Request camera permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            // Camera preview
            val preview = Preview.Builder().build()

            preview.surfaceProvider =
                previewView.surfaceProvider

            // Analyze camera frames
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(
                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                )
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

                var bitmap: Bitmap? = null
                var rotatedBitmap: Bitmap? = null

                try {
                    bitmap = imageProxy.toBitmap()

                    rotatedBitmap = rotateBitmap(
                        bitmap,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    val result = handHelper.detect(
                        rotatedBitmap,
                        imageProxy.imageInfo.timestamp / 1_000_000
                    )

                    val frameWidth = rotatedBitmap.width
                    val frameHeight = rotatedBitmap.height

                    runOnUiThread {
                        overlayView.setResults(result, frameWidth, frameHeight)
                    }

                } catch (e: Exception) {
                    Log.e("HandDetection", "Frame processing failed", e)

                } finally {
                    if (rotatedBitmap != null &&
                        rotatedBitmap !== bitmap
                    ) {
                        rotatedBitmap.recycle()
                    }

                    bitmap?.recycle()
                    imageProxy.close()
                }
            }

            val cameraSelector =
                CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun rotateBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int
    ): Bitmap {

        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::handHelper.isInitialized) {
            handHelper.close()
        }

        cameraExecutor.shutdown()
    }
}
