package com.ahlikomputerit.lumentransfer.presentation.receive

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameEnvelope
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import com.ahlikomputerit.lumentransfer.domain.protocol.MetadataFrameCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiveViewModelTest {
    @Test
    fun `duplicate sequence is counted without inflating received frames`() {
        val viewModel = ReceiveViewModel()
        val frame = frame(TransferId(ByteArray(16) { 1 }))

        viewModel.onEnvelope(frame)
        viewModel.onEnvelope(frame)

        val state = viewModel.uiState.value
        assertEquals(1, state.receivedFrames)
        assertEquals(1, state.duplicateFrames)
        assertEquals(0, state.rejectedFrames)
    }

    @Test
    fun `different transfer id is rejected after session starts`() {
        val viewModel = ReceiveViewModel()
        viewModel.onEnvelope(frame(TransferId(ByteArray(16) { 1 })))
        viewModel.onEnvelope(frame(TransferId(ByteArray(16) { 2 })))

        val state = viewModel.uiState.value
        assertEquals(1, state.receivedFrames)
        assertEquals(1, state.rejectedFrames)
    }

    @Test
    fun `permission result controls scanning state`() {
        val viewModel = ReceiveViewModel()
        viewModel.onPermissionResult(granted = false, canAskAgain = true)
        assertEquals(CameraPermissionState.Denied, viewModel.uiState.value.permission)
        viewModel.onPermissionResult(granted = true)
        assertEquals(CameraPermissionState.Granted, viewModel.uiState.value.permission)
        assertEquals(true, viewModel.uiState.value.isScanning)
    }

    private fun frame(id: TransferId) = FrameEnvelope(
        version = 1,
        flags = 0,
        transferId = id,
        kind = FrameKind.META,
        seed = 0,
        degree = 0,
        sequence = 0,
        payload = MetadataFrameCodec.encode(
            FileMetadata(
                transferId = id,
                fileName = "fixture.bin",
                mimeType = "application/octet-stream",
                sizeBytes = 1,
                sha256 = ByteArray(32),
                blockSize = 256,
                sourceBlockCount = 1,
            ),
        ),
        frameCrc32 = 0u,
    )
}
