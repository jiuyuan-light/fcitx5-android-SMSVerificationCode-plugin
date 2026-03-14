package org.fcitx.fcitx5.android.plugin.sms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log

object ClipboardUtil {
    fun copy(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Verification Code", text)
            clipboard.setPrimaryClip(clip)
            Log.i("ClipboardUtil", "Copied to clipboard: $text")
        } catch (e: Exception) {
            Log.e("ClipboardUtil", "Failed to copy to clipboard", e)
        }
    }
}
