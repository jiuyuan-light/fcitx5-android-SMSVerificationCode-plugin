package org.fcitx.fcitx5.android.plugin.sms

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import android.os.SystemClock
import android.provider.Settings
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.plugin.sms.databinding.ActivityPluginBinding
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

private const val PREFS_NAME = "otp"
private const val PREF_KEYWORDS = "keywords"
private const val REQUEST_SMS_PERMISSION = 100
private const val CLIP_LABEL = "OTP"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFS_NAME)
private val KEYWORDS_KEY = stringPreferencesKey(PREF_KEYWORDS)

private val DIGIT_PATTERN = Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
private val DEFAULT_KEYWORDS = listOf(
    "验证码",
    "校验码",
    "动态码",
    "确认码",
    "取件码",
    "提货码",
    "一次性",
    "口令"
)
private val LENGTH_PREFERENCE = intArrayOf(6, 4, 5, 7, 8)

private object OtpDeduper {
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

internal fun parseKeywords(raw: String): List<String> {
    return raw.split(Regex("[,;\\n]+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

private object KeywordStore {
    private val initialized = AtomicBoolean(false)
    @Volatile private var cachedRaw: String = ""
    @Volatile private var cached: List<String> = DEFAULT_KEYWORDS

    private fun update(raw: String) {
        val parsed = parseKeywords(raw)
        cachedRaw = raw
        cached = if (parsed.isEmpty()) DEFAULT_KEYWORDS else parsed
    }

    fun ensureLoaded(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        MainService.scope.launch {
            val raw = context.applicationContext.dataStore.data.first()[KEYWORDS_KEY].orEmpty()
            update(raw)
        }
    }

    fun keywords(context: Context): List<String> {
        ensureLoaded(context)
        return cached
    }

    fun keywordsText(context: Context): String {
        ensureLoaded(context)
        return if (cachedRaw.isNotBlank()) cachedRaw else DEFAULT_KEYWORDS.joinToString(", ")
    }

    fun save(context: Context, raw: String) {
        val trimmed = raw.trim()
        update(trimmed)
        MainService.scope.launch {
            context.applicationContext.dataStore.edit { prefs -> prefs[KEYWORDS_KEY] = trimmed }
        }
    }
}

internal fun pickOtp(text: String, keywords: List<String>): String? {
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

private fun extractSmsBodies(intent: Intent): List<String> {
    return Telephony.Sms.Intents.getMessagesFromIntent(intent)
        ?.mapNotNull { it.messageBody }
        ?: emptyList()
}

private fun extractNotificationText(sbn: StatusBarNotification?): String? {
    val extras = sbn?.notification?.extras ?: return null
    val parts = ArrayList<CharSequence>(5)
    extras.getCharSequence(Notification.EXTRA_TITLE)?.let { parts.add(it) }
    extras.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it) }
    extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it) }
    extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let { parts.addAll(it) }
    val content = parts.joinToString(" ") { it.toString() }.trim()
    return content.ifEmpty { null }
}

fun Context.processAndCopyCode(text: String) {
    val code = pickOtp(text, KeywordStore.keywords(this)) ?: return
    if (!OtpDeduper.shouldCopy(code)) return
    try {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, code))
    } catch (e: Exception) {
        Log.e("Fcitx5Sms", "Copy failed", e)
    }
}

class MainService : Service() {
    companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onBind(intent: Intent): IBinder = Messenger(Handler(Looper.getMainLooper())).binder
}

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == action ||
            Telephony.Sms.Intents.SMS_DELIVER_ACTION == action
        ) {
            extractSmsBodies(intent).forEach { context.processAndCopyCode(it) }
        }
    }
}

class OtpNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val content = extractNotificationText(sbn) ?: return
            processAndCopyCode(content)
        } catch (t: Throwable) {
            Log.e("Fcitx5Sms", "Notification parse failed", t)
        }
    }
}

class PluginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPluginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        KeywordStore.ensureLoaded(this)

        binding.keywordInput.setText(KeywordStore.keywordsText(this))
        binding.saveKeywordsButton.setOnClickListener {
            KeywordStore.save(this, binding.keywordInput.text?.toString().orEmpty())
            Toast.makeText(this, getString(R.string.keywords_saved), Toast.LENGTH_SHORT).show()
        }
        binding.smsPermissionButton.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_SMS_PERMISSION)
        }
        binding.notificationPermissionButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_SMS_PERMISSION) {
            val ok = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            Toast.makeText(
                this,
                if (ok) getString(R.string.permission_granted) else getString(R.string.permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
