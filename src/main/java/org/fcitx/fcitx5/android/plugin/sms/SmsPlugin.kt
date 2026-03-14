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
import android.widget.Toast
import org.fcitx.fcitx5.android.plugin.sms.databinding.ActivityPluginBinding

private const val REQUEST_SMS_PERMISSION = 100

fun Context.processAndCopyCode(text: String) {
    val code = OtpParser.pickOtp(text, KeywordStore.keywords(this)) ?: return
    if (!OtpDeduper.shouldCopy(code)) return
    try {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText(getString(R.string.clip_label), code))
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
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == action) {
            MessageExtractors.extractSmsBodies(intent).forEach { context.processAndCopyCode(it) }
        }
    }
}

class OtpNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val content = MessageExtractors.extractNotificationText(sbn) ?: return
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
