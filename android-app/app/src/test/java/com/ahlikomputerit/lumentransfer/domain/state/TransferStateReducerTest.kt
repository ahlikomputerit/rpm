package com.ahlikomputerit.lumentransfer.domain.state

import com.ahlikomputerit.lumentransfer.domain.model.FileMetadata
import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferError
import com.ahlikomputerit.lumentransfer.domain.model.TransferId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransferStateReducerTest {
    private val metadata = FileMetadata(
        transferId = TransferId(ByteArray(16) { it.toByte() }),
        fileName = "fixture.bin",
        mimeType = "application/octet-stream",
        sizeBytes = 2_048,
        sha256 = ByteArray(32),
        blockSize = 1_024,
        sourceBlockCount = 2,
    )

    @Test
    fun `sender lifecycle preserves immutable state transitions`() {
        val idle = TransferState(TransferRole.SEND)
        val prepared = reduceTransferState(idle, TransferEvent.FilePrepared(metadata, 10))
        val sending = reduceTransferState(prepared, TransferEvent.SenderStarted(20, 5_000))
        val emitted = reduceTransferState(sending, TransferEvent.FrameEmitted(FrameKind.META, 0, 30))
        val paused = reduceTransferState(emitted, TransferEvent.SenderPaused(40))
        val resumed = reduceTransferState(paused, TransferEvent.SenderResumed(50, 5_000))

        assertEquals(TransferPhase.PREPARING, prepared.phase)
        assertEquals(TransferPhase.SENDING, sending.phase)
        assertEquals(1, emitted.emittedFrames)
        assertEquals(TransferPhase.PAUSED, paused.phase)
        assertEquals(TransferPhase.SENDING, resumed.phase)
        assertEquals(5_050L, resumed.timeoutDeadlineMs)
    }

    @Test
    fun `receiver timeout and rotation are explicit`() {
        val scanning = reduceTransferState(
            TransferState(TransferRole.RECEIVE),
            TransferEvent.ReceiverStarted(100, 2_000),
        )
        val rotated = reduceTransferState(scanning, TransferEvent.RotationChanged(450, 200))
        val timeout = reduceTransferState(rotated, TransferEvent.Timeout(2_101))

        assertEquals(TransferPhase.SCANNING, scanning.phase)
        assertEquals(90, rotated.rotationDegrees)
        assertEquals(TransferPhase.FAILED, timeout.phase)
        assertEquals(TransferError.DECODER_STALLED, timeout.error)
        assertNull(timeout.timeoutDeadlineMs)
    }

    @Test
    fun `receiver accepted frame can become ready then saved`() {
        val received = reduceTransferState(
            TransferState(TransferRole.RECEIVE),
            TransferEvent.FrameAccepted(metadata.transferId, FrameKind.SYSTEMATIC_DATA, 1, 2, 2, 3, 100),
        )
        val ready = reduceTransferState(received, TransferEvent.ReadyToSave(200))
        val saved = reduceTransferState(ready, TransferEvent.Saved(300))

        assertEquals(TransferPhase.RECEIVING, received.phase)
        assertEquals(2, received.recoveredBlocks)
        assertEquals(3, received.equationCount)
        assertEquals(TransferPhase.READY_TO_SAVE, ready.phase)
        assertEquals(TransferPhase.SAVED, saved.phase)
    }
}
