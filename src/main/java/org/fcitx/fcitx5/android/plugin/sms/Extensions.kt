package org.fcitx.fcitx5.android.plugin.sms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log

fun Context.copyToClipboard(text: String, label: String = "Verification Code") {
    try {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Log.i("Fcitx5SmsPlugin", "Copied to clipboard: $text")
    } catch (e: Exception) {
        Log.e("Fcitx5SmsPlugin", "Failed to copy to clipboard", e)
    }
}
