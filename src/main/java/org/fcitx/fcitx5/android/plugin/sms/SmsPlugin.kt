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
import org.fcitx.fcitx5.android.plugin.sms.databinding.ActivityPluginBinding
import java.util.regex.Pattern

private const val REQUEST_SMS_PERMISSION = 100
private const val CLIP_LABEL = "OTP"

private val DIGIT_PATTERN = Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
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

        binding.keywordInput.setText(KeywordStore.keywordsText(this))
        binding.saveKeywordsButton.setOnClickListener {
            KeywordStore.save(this, binding.keywordInput.text?.toString().orEmpty())
            Toast.makeText(this, getString(R.string.keywords_saved), Toast.LENGTH_SHORT).show()
        }
        binding.resetKeywordsButton.setOnClickListener {
            KeywordStore.reset(this)
            binding.keywordInput.setText(KeywordStore.keywordsText(this))
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
