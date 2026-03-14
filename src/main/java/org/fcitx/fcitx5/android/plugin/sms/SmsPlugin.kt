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
import android.provider.Telephony
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.regex.Pattern

private val DIGIT_PATTERN = Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
private const val PREFS_NAME = "otp"
private const val PREF_KEYWORDS = "keywords"
private const val REQUEST_SMS_PERMISSION = 100
private const val CLIP_LABEL = "OTP"
private val DEFAULT_KEYWORDS = listOf(
    "otp",
    "passcode",
    "one-time",
    "verification",
    "code",
    "sms code",
    "auth code"
)

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

private fun loadKeywords(context: Context): List<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(PREF_KEYWORDS, null)?.trim().orEmpty()
    if (raw.isEmpty()) return DEFAULT_KEYWORDS
    val parsed = raw.split(Regex("[,;\\n]+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return if (parsed.isEmpty()) DEFAULT_KEYWORDS else parsed
}

private fun loadKeywordsText(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(PREF_KEYWORDS, null)?.trim().orEmpty()
    return if (raw.isEmpty()) DEFAULT_KEYWORDS.joinToString(", ") else raw
}

private fun saveKeywords(context: Context, raw: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(PREF_KEYWORDS, raw.trim()).apply()
}

private fun pickOtp(text: String, keywords: List<String>): String? {
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

    return matches.firstOrNull { (_, code) -> code.length == 6 }?.second ?: matches[0].second
}

fun Context.processAndCopyCode(text: String) {
    val code = pickOtp(text, loadKeywords(this)) ?: return
    if (!OtpDeduper.shouldCopy(code)) return
    try {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, code))
    } catch (e: Exception) {
        Log.e("Fcitx5Sms", "Copy failed", e)
    }
}

class MainService : Service() {
    override fun onBind(intent: Intent): IBinder = Messenger(Handler(Looper.getMainLooper())).binder
}

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == action ||
            Telephony.Sms.Intents.SMS_DELIVER_ACTION == action
        ) {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
                ?.forEach { context.processAndCopyCode(it.messageBody) }
        }
    }
}

class OtpNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val extras = sbn?.notification?.extras ?: return
            val parts = ArrayList<CharSequence>(5)
            extras.getCharSequence(Notification.EXTRA_TITLE)?.let { parts.add(it) }
            extras.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it) }
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it) }
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let { parts.addAll(it) }
            val content = parts.joinToString(" ") { it.toString() }.trim()
            if (content.isNotEmpty()) processAndCopyCode(content)
        } catch (t: Throwable) {
            Log.e("Fcitx5Sms", "Notification parse failed", t)
        }
    }
}

class PluginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }
        root.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 20f
            setPadding(0, 0, 0, 48)
        })
        root.addView(TextView(this).apply {
            text = getString(R.string.keyword_label)
            textSize = 14f
            setPadding(0, 0, 0, 12)
        })
        val keywordInput = EditText(this).apply {
            hint = getString(R.string.keyword_hint)
            setText(loadKeywordsText(this@PluginActivity))
            setPadding(0, 0, 0, 24)
        }
        root.addView(keywordInput)
        root.addView(Button(this).apply {
            text = getString(R.string.save_keywords)
            setOnClickListener {
                saveKeywords(this@PluginActivity, keywordInput.text?.toString().orEmpty())
                Toast.makeText(this@PluginActivity, getString(R.string.keywords_saved), Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.grant_sms_permission)
            setOnClickListener { requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_SMS_PERMISSION) }
        })
        root.addView(Button(this).apply {
            text = getString(R.string.grant_notification_permission)
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        setContentView(root)
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
