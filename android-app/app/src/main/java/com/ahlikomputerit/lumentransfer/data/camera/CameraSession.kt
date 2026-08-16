package com.ahlikomputerit.lumentransfer.data.camera

/**
 * Camera boundary for the next checkpoint. The implementation must bind CameraX
 * Preview and ImageAnalysis, close every ImageProxy, and never expose camera
 * buffers to the domain layer.
 */
interface CameraSession {
    fun start(onFrame: (ByteArray) -> Unit)
    fun stop()
}

class PlaceholderCameraSession : CameraSession {
    override fun start(onFrame: (ByteArray) -> Unit) = Unit
    override fun stop() = Unit
}
