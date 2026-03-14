package org.fcitx.fcitx5.android.plugin.sms

import android.os.SystemClock
import java.util.regex.Pattern

private val DIGIT_PATTERN = Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
private val LENGTH_PREFERENCE = intArrayOf(6, 4, 5, 7, 8)

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

internal object OtpParser {
    fun pickOtp(text: String, keywords: List<String>): String? {
        val matches = ArrayList<Pair<Int, String>>(2)
        val matcher = DIGIT_PATTERN.matcher(text)
        while (matcher.find()) {
            val code = matcher.group(1) ?: continue
            matches.add(matcher.start(1) to code)
            if (matches.size >= 8) break
        }
        if (matches.isEmpty()) return null
        if (matches.size == 1) return matches[0].second

        if (keywords.isNotEmpty()) {
            val lower = text.lowercase()
            val keywordPos = keywords
                .map { lower.indexOf(it.lowercase()) }
                .filter { it >= 0 }
            if (keywordPos.isNotEmpty()) {
                val minKeywordPos = keywordPos.minOrNull() ?: -1
                return matches.minByOrNull { (pos, _) -> kotlin.math.abs(pos - minKeywordPos) }?.second
            }
        }

        for (len in LENGTH_PREFERENCE) {
            val picked = matches.firstOrNull { (_, code) -> code.length == len }?.second
            if (picked != null) return picked
        }
        return matches[0].second
    }
}
