package org.fcitx.fcitx5.android.plugin.sms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import java.util.regex.Pattern

private val PATTERNS = listOf(
    // Chinese "验证码是123456" or "验证码：123456"
    Pattern.compile("验证码[:：\\s]*([0-9]{4,8})"),
    // English "code is 123456"
    Pattern.compile("code\\s*is\\s*([0-9]{4,8})", Pattern.CASE_INSENSITIVE),
    // 4-8 digits, possibly surrounded by non-digits (standard format)
    Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
)

fun Context.processAndCopyCode(text: String) {
    val lowerText = text.lowercase()
    if (!lowerText.contains("code") && !lowerText.contains("验证码") && !lowerText.contains("verification")) return

    for (pattern in PATTERNS) {
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val code = matcher.group(1) ?: continue
            copyToClipboard(code)
            return
        }
    }
}

private fun Context.copyToClipboard(text: String) {
    try {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Verification Code", text)
        clipboard.setPrimaryClip(clip)
        Log.i("Fcitx5SmsPlugin", "Copied to clipboard: $text")
    } catch (e: Exception) {
        Log.e("Fcitx5SmsPlugin", "Failed to copy to clipboard", e)
    }
}
