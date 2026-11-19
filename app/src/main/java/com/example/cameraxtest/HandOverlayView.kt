
package com.example.cameraxtest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.max

class HandOverlayView(context: Context) : View(context) {

    private val pointPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private var landmarks =
        emptyList<NormalizedLandmark>()

    // Dimensions (in the same orientation as the on-screen preview) of the
    // bitmap that was actually fed to the hand landmarker.
    private var imageWidth = 0
    private var imageHeight = 0

    // The preview uses the front camera, which PreviewView mirrors
    // horizontally. The analyzed frame is not mirrored, so we flip the
    // drawn points to line back up with what's on screen.
    var mirrorHorizontally = true

    private val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        5 to 9, 9 to 10, 10 to 11, 11 to 12,
        9 to 13, 13 to 14, 14 to 15, 15 to 16,
        13 to 17, 17 to 18, 18 to 19, 19 to 20,
        0 to 17
    )

    fun setResults(
        result: HandLandmarkerResult?,
        imageWidth: Int,
        imageHeight: Int
    ) {
        landmarks = result?.landmarks()?.firstOrNull() ?: emptyList()
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (landmarks.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return

        // PreviewView center-crops the camera stream to fill the view while
        // preserving aspect ratio. Replicate that exact scale/offset here so
        // landmarks land on the hand instead of being stretched across the
        // whole (differently-shaped) view.
        val scale = max(
            width.toFloat() / imageWidth,
            height.toFloat() / imageHeight
        )
        val offsetX = (width - imageWidth * scale) / 2f
        val offsetY = (height - imageHeight * scale) / 2f

        val points = landmarks.map { landmark ->
            var x = offsetX + landmark.x() * imageWidth * scale
            val y = offsetY + landmark.y() * imageHeight * scale
            if (mirrorHorizontally) x = width - x
            Pair(x, y)
        }

        connections.forEach { (start, end) ->
            canvas.drawLine(
                points[start].first,
                points[start].second,
                points[end].first,
                points[end].second,
                linePaint
            )
        }

        points.forEach { (x, y) ->
            canvas.drawCircle(x, y, 12f, pointPaint)
        }
    }
}
