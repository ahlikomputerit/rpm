package com.ahlikomputerit.lumentransfer.domain.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiagnosticsStore(role: com.ahlikomputerit.lumentransfer.domain.state.TransferRole) {
    private val _snapshot = MutableStateFlow(TransferDiagnostics(role))
    val snapshot: StateFlow<TransferDiagnostics> = _snapshot.asStateFlow()

    fun dispatch(event: DiagnosticsEvent): TransferDiagnostics {
        while (true) {
            val current = _snapshot.value
            val next = reduceDiagnostics(current, event)
            if (_snapshot.compareAndSet(current, next)) return next
        }
    }
}
