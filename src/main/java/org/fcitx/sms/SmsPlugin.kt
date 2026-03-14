package org.fcitx.sms

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

private val CODE_PATTERN = Pattern.compile("(?:验证码[:：\\s]*|code\\s*is\\s*|(?<![0-9]))([0-9]{4,8})(?![0-9])", Pattern.CASE_INSENSITIVE)

fun Context.processAndCopyCode(text: String) {
    if (!text.lowercase().let { it.contains("code") || it.contains("验证码") || it.contains("verification") }) return
    CODE_PATTERN.matcher(text).takeIf { it.find() }?.group(1)?.let { code ->
        try {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("OTP", code))
            Log.i("Fcitx5Sms", "Copied: $code")
        } catch (e: Exception) { Log.e("Fcitx5Sms", "Copy failed") }
    }
}

class MainService : Service() {
    override fun onBind(intent: Intent): IBinder = Messenger(Handler(Looper.getMainLooper())).binder
}

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)?.forEach { context.processAndCopyCode(it.messageBody) }
        }
    }
}

class OtpNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.notification?.extras?.let { e ->
            val content = "${e.getString("android.title") ?: ""} ${e.getString("android.text") ?: ""} ${e.getString("android.bigText") ?: ""}"
            if (content.isNotBlank()) processAndCopyCode(content)
        }
    }
}

class PluginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(64, 64, 64, 64) }
        root.addView(TextView(this).apply { text = "短信验证码插件"; textSize = 24f; setPadding(0, 0, 0, 32) })
        root.addView(TextView(this).apply { text = "自动提取短信验证码并复制到剪贴板"; textSize = 16f; setPadding(0, 0, 0, 64) })
        root.addView(Button(this).apply { text = "授予短信权限"; setOnClickListener { requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 100) } })
        root.addView(Button(this).apply { text = "授予通知监听权限"; setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } })
        setContentView(root)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100) Toast.makeText(this, if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) "权限已授予" else "权限被拒绝", Toast.LENGTH_SHORT).show()
    }
}
