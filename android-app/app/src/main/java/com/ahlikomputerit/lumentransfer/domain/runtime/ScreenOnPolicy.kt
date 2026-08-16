package com.ahlikomputerit.lumentransfer.domain.runtime

interface ScreenOnPolicy {
    fun acquire()
    fun release()
}
