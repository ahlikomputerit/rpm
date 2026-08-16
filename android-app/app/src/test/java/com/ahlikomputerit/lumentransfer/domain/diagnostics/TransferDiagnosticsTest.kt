package com.ahlikomputerit.lumentransfer.domain.diagnostics

import com.ahlikomputerit.lumentransfer.domain.model.FrameKind
import com.ahlikomputerit.lumentransfer.domain.model.TransferError
import com.ahlikomputerit.lumentransfer.domain.state.TransferPhase
import com.ahlikomputerit.lumentransfer.domain.state.TransferRole
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferDiagnosticsTest {
    @Test
    fun `diagnostics counts payload-free frame metrics and calculates goodput`() {
        val started = reduceDiagnostics(TransferDiagnostics(TransferRole.RECEIVE), DiagnosticsEvent.Started(1_000))
        val accepted = reduceDiagnostics(
            started,
            DiagnosticsEvent.FrameAccepted(FrameKind.REPAIR_DATA, 7, 1_024, 12, 8, 10, 1_500),
        )
        val completed = reduceDiagnostics(accepted, DiagnosticsEvent.Completed(2_500))

        assertEquals(1L, completed.acceptedFrames)
        assertEquals(1_024L, completed.acceptedBytes)
        assertEquals(1L, completed.repairFrames)
        assertEquals(12, completed.sourceBlocks)
        assertEquals(8, completed.recoveredBlocks)
        assertEquals(10, completed.equationCount)
        assertEquals(1_500L, completed.elapsedMs)
        assertEquals(682.666, completed.goodputBytesPerSecond, 0.001)
        assertEquals(TransferPhase.SAVED, completed.terminalPhase)
    }

    @Test
    fun `diagnostics tracks emitted systematic and repair frames`() {
        var state = reduceDiagnostics(TransferDiagnostics(TransferRole.SEND), DiagnosticsEvent.Started(10))
        state = reduceDiagnostics(state, DiagnosticsEvent.FrameEmitted(FrameKind.SYSTEMATIC_DATA, 1, 500, 20))
        state = reduceDiagnostics(state, DiagnosticsEvent.FrameEmitted(FrameKind.REPAIR_DATA, 2, 600, 30))
        state = reduceDiagnostics(state, DiagnosticsEvent.Duplicate(40))
        state = reduceDiagnostics(state, DiagnosticsEvent.Rejected(50))
        state = reduceDiagnostics(state, DiagnosticsEvent.Failed(TransferError.DECODER_STALLED, 60))

        assertEquals(2L, state.emittedFrames)
        assertEquals(1_100L, state.emittedBytes)
        assertEquals(1L, state.systematicFrames)
        assertEquals(1L, state.repairFrames)
        assertEquals(1L, state.duplicateFrames)
        assertEquals(1L, state.rejectedFrames)
        assertEquals(TransferPhase.FAILED, state.terminalPhase)
        assertEquals(TransferError.DECODER_STALLED, state.error)
    }

    @Test
    fun `reset removes previous diagnostic session`() {
        var state = reduceDiagnostics(TransferDiagnostics(TransferRole.SEND), DiagnosticsEvent.Started(1))
        state = reduceDiagnostics(state, DiagnosticsEvent.FrameEmitted(FrameKind.META, 0, 100, 2))
        state = reduceDiagnostics(state, DiagnosticsEvent.Reset(3))

        assertEquals(TransferRole.SEND, state.role)
        assertEquals(0L, state.emittedFrames)
        assertEquals(TransferPhase.IDLE, state.terminalPhase)
        assertEquals(0L, state.elapsedMs)
    }
}
