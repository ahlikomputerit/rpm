package com.ahlikomputerit.lumentransfer.domain.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransferStore(initial: TransferState) {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<TransferState> = _state.asStateFlow()

    fun dispatch(event: TransferEvent): TransferState {
        while (true) {
            val current = _state.value
            val next = reduceTransferState(current, event)
            if (_state.compareAndSet(current, next)) return next
        }
    }
}
