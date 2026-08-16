package com.ahlikomputerit.lumentransfer.data.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface CameraSession {
    fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrame: (CameraRgbaFrame) -> Unit,
        onError: (Throwable) -> Unit,
    )

    fun unbind()
}

data class CameraRgbaFrame(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val rowStride: Int,
    val pixelStride: Int,
    val rotationDegrees: Int,
)

class CameraXSession(context: Context) : CameraSession, AutoCloseable {
    private val appContext = context.applicationContext
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null

    override fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrame: (CameraRgbaFrame) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        unbind()
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        val providerFuture = ProcessCameraProvider.getInstance(appContext)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider
                val preview = Preview.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyze(imageProxy, onFrame, onError)
                }
                analysis = imageAnalysis
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            } catch (error: Throwable) {
                onError(error)
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    override fun unbind() {
        analysis?.clearAnalyzer()
        analysis = null
        cameraProvider?.unbindAll()
    }

    override fun close() {
        unbind()
        cameraExecutor.shutdownNow()
    }

    private fun analyze(
        imageProxy: ImageProxy,
        onFrame: (CameraRgbaFrame) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        try {
            val plane = imageProxy.planes.firstOrNull() ?: return
            val buffer = plane.buffer.duplicate().apply { rewind() }
            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
            onFrame(
                CameraRgbaFrame(
                    bytes = bytes,
                    width = imageProxy.width,
                    height = imageProxy.height,
                    rowStride = plane.rowStride,
                    pixelStride = plane.pixelStride,
                    rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                ),
            )
        } catch (error: Throwable) {
            onError(error)
        } finally {
            imageProxy.close()
        }
    }
}
