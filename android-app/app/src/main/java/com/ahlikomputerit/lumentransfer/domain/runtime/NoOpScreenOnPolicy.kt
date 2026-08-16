package com.ahlikomputerit.lumentransfer.domain.runtime

class NoOpScreenOnPolicy : ScreenOnPolicy {
    override fun acquire() = Unit
    override fun release() = Unit
}
