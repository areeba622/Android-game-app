package com.example.cameraxtest

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    private val context: Context
) {

    private var handLandmarker: HandLandmarker? = null

    fun setup() {

        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumHands(1)
            .build()

        handLandmarker =
            HandLandmarker.createFromOptions(context, options)
    }

    fun detect(
        bitmap: Bitmap,
        timestamp: Long
    ): HandLandmarkerResult? {

        val mpImage = BitmapImageBuilder(bitmap).build()

        return handLandmarker?.detectForVideo(
            mpImage,
            timestamp
        )
    }

    fun close() {
        handLandmarker?.close()
        handLandmarker = null
    }
}
