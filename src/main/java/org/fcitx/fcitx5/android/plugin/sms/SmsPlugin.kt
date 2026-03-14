package org.fcitx.fcitx5.android.plugin.sms

import android.Manifest
import android.app.Activity
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

/* --- Extensions & Extraction --- */

private val PATTERNS = listOf(
    Pattern.compile("验证码[:：\\s]*([0-9]{4,8})"),
    Pattern.compile("code\\s*is\\s*([0-9]{4,8})", Pattern.CASE_INSENSITIVE),
    Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")
)

fun Context.processAndCopyCode(text: String) {
    val lowerText = text.lowercase()
    if (!lowerText.contains("code") && !lowerText.contains("验证码") && !lowerText.contains("verification")) return

    for (pattern in PATTERNS) {
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val code = matcher.group(1) ?: continue
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Verification Code", code)
                clipboard.setPrimaryClip(clip)
                Log.i("Fcitx5SmsPlugin", "Copied to clipboard: $code")
            } catch (e: Exception) {
                Log.e("Fcitx5SmsPlugin", "Failed to copy to clipboard", e)
            }
            return
        }
    }
}

/* --- Components --- */

class MainService : Service() {
    override fun onBind(intent: Intent): IBinder = Messenger(Handler(Looper.getMainLooper())).binder
}

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                messages?.forEach { sms ->
                    context.processAndCopyCode(sms.messageBody)
                }
            }
        } catch (t: Throwable) {
            Log.e("Fcitx5SmsPlugin", "Error processing SMS", t)
        }
    }
}

class OtpNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val notification = sbn?.notification ?: return
            val extras = notification.extras ?: return
            val title = extras.getString("android.title")
            val text = extras.getString("android.text")
            val bigText = extras.getString("android.bigText")
            val content = "${title ?: ""} ${text ?: ""} ${bigText ?: ""}"
            if (content.isNotBlank()) {
                processAndCopyCode(content)
            }
        } catch (t: Throwable) {
            Log.e("Fcitx5SmsPlugin", "Error in onNotificationPosted", t)
        }
    }
}

class PluginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
            layoutParams = LinearLayout.LayoutParams(-1, -1)
        }
        val titleTv = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(titleTv)
        val descTv = TextView(this).apply {
            text = getString(R.string.description)
            textSize = 16f
            setPadding(0, 0, 0, 64)
        }
        rootLayout.addView(descTv)
        val btnSms = Button(this).apply {
            text = getString(R.string.grant_sms_permission)
            setOnClickListener {
                requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 100)
            }
        }
        rootLayout.addView(btnSms)
        val btnNotification = Button(this).apply {
            text = getString(R.string.grant_notification_permission)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        rootLayout.addView(btnNotification)
        setContentView(rootLayout)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100) {
            val message = if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                getString(R.string.permission_granted)
            } else {
                getString(R.string.permission_denied)
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
