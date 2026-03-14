package org.fcitx.fcitx5.android.plugin.sms

import java.util.regex.Pattern

object VerificationCodeExtractor {
    private val PATTERNS = listOf(
        // Chinese "验证码是123456" or "验证码：123456"
        Pattern.compile("验证码[:：\\s]*([0-9]{4,8})"),
        // English "code is 123456"
        Pattern.compile("code\\s*is\\s*([0-9]{4,8})", Pattern.CASE_INSENSITIVE),
        // 4-8 digits, possibly surrounded by non-digits (standard format)
        Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
    )

    fun extract(text: String): String? {
        val lowerText = text.lowercase()
        val isVerification = lowerText.contains("code") || lowerText.contains("验证码") || lowerText.contains("verification")
        
        if (!isVerification) return null

        for (pattern in PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        
        return null
    }
}
