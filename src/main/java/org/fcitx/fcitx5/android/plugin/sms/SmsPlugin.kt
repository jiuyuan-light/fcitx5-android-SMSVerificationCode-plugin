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
import android.provider.Settings
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.regex.Pattern

private val DIGIT_PATTERN = Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
private val KEYWORDS = arrayOf("验证码", "校验码", "动态码", "确认码", "取件码", "提货码", "一次性", "口令", "otp", "passcode", "one-time", "verification", "code")

private fun pickOtp(text: String): String? {
    val matches = ArrayList<Pair<Int, String>>(2)
    val matcher = DIGIT_PATTERN.matcher(text)
    while (matcher.find()) {
        val code = matcher.group(1) ?: continue
        matches.add(matcher.start(1) to code)
        if (matches.size >= 8) break
    }
    if (matches.isEmpty()) return null
    if (matches.size == 1) return matches[0].second

    val lower = text.lowercase()
    val keywordPos = KEYWORDS.map { lower.indexOf(it) }.filter { it >= 0 }
    if (keywordPos.isNotEmpty()) {
        val minKeywordPos = keywordPos.minOrNull() ?: -1
        return matches.minByOrNull { (pos, _) -> kotlin.math.abs(pos - minKeywordPos) }?.second
    }

    return matches.firstOrNull { (_, code) -> code.length == 6 }?.second ?: matches[0].second
}

fun Context.processAndCopyCode(text: String) {
    val code = pickOtp(text) ?: return
    try {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("OTP", code))
        Log.i("Fcitx5Sms", "Copied: $code")
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
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == action || Telephony.Sms.Intents.SMS_DELIVER_ACTION == action) {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)?.forEach { context.processAndCopyCode(it.messageBody) }
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
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(64, 64, 64, 64) }
        root.addView(TextView(this).apply { text = getString(R.string.app_name); textSize = 24f; setPadding(0, 0, 0, 32) })
        root.addView(TextView(this).apply { text = "自动提取短信验证码并复制到剪贴板"; textSize = 16f; setPadding(0, 0, 0, 64) })
        root.addView(Button(this).apply { text = getString(R.string.grant_sms_permission); setOnClickListener { requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 100) } })
        root.addView(Button(this).apply { text = getString(R.string.grant_notification_permission); setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } })
        setContentView(root)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100) Toast.makeText(this, if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) getString(R.string.permission_granted) else getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
    }
}
