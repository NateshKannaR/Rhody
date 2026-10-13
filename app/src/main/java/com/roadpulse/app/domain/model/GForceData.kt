package com.roadpulse.app.domain.model

data class GForceData(
    val lateralG: Float = 0f,      // Left (-) / Right (+) cornering
    val longitudinalG: Float = 0f, // Braking (-) / Acceleration (+)
    val maxG: Float = 0f
)
