package com.ahlikomputerit.lumentransfer.app

import android.view.Window
import android.view.WindowManager
import com.ahlikomputerit.lumentransfer.domain.runtime.ScreenOnPolicy

class WindowScreenOnPolicy(private val window: Window) : ScreenOnPolicy {
    override fun acquire() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun release() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
