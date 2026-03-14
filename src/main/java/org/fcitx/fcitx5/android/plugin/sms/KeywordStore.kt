package org.fcitx.fcitx5.android.plugin.sms

import android.content.Context

private const val PREFS_NAME = "otp"
private const val PREF_KEYWORDS = "keywords"

private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

internal fun parseKeywords(raw: String): List<String> {
    return raw.split(Regex("[,;\\n]+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

internal object KeywordStore {
    @Volatile private var cachedRaw: String = ""
    @Volatile private var cached: List<String> = emptyList()
    private val lock = Any()

    private fun defaultKeywords(context: Context): List<String> {
        return try {
            parseKeywords(context.getString(R.string.default_keywords))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun update(context: Context, raw: String) {
        val parsed = parseKeywords(raw)
        cachedRaw = raw
        val fallback = defaultKeywords(context)
        cached = if (parsed.isEmpty()) fallback else parsed
    }

    fun keywords(context: Context): List<String> {
        val raw = prefs(context.applicationContext).getString(PREF_KEYWORDS, "")?.trim().orEmpty()
        synchronized(lock) {
            if (cached.isEmpty() || raw != cachedRaw) {
                update(context.applicationContext, raw)
            }
            return cached
        }
    }

    fun keywordsText(context: Context): String {
        val raw = prefs(context.applicationContext).getString(PREF_KEYWORDS, "")?.trim().orEmpty()
        val fallback = defaultKeywords(context.applicationContext)
        return if (raw.isNotBlank()) raw else fallback.joinToString(", ")
    }

    fun save(context: Context, raw: String) {
        val trimmed = raw.trim()
        val app = context.applicationContext
        synchronized(lock) {
            update(app, trimmed)
            prefs(app).edit().putString(PREF_KEYWORDS, trimmed).apply()
        }
    }

    fun reset(context: Context) {
        val app = context.applicationContext
        synchronized(lock) {
            cachedRaw = ""
            cached = defaultKeywords(app)
            prefs(app).edit().remove(PREF_KEYWORDS).apply()
        }
    }
}
