package org.fcitx.fcitx5.android.plugin.sms

import android.os.SystemClock

internal object OtpDeduper {
    private var lastCode: String? = null
    private var lastAt: Long = 0
    private const val WINDOW_MS = 60_000L

    @Synchronized
    fun shouldCopy(code: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val same = (code == lastCode) && (now - lastAt) < WINDOW_MS
        if (same) return false
        lastCode = code
        lastAt = now
        return true
    }
}
