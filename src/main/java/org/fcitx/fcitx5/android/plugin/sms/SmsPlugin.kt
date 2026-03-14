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
import android.os.SystemClock
import android.provider.Telephony
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.regex.Pattern

private val DIGIT_PATTERN = Pattern.compile("(?<![0-9])([0-9]{4,8})(?![0-9])")

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
    return matches.firstOrNull { (_, code) -> code.length == 6 }?.second ?: matches[0].second
}

fun Context.processAndCopyCode(text: String) {
    val code = pickOtp(text) ?: return
    if (!OtpDeduper.shouldCopy(code)) return
    try {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("OTP", code))
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
        root.addView(Button(this).apply {
            text = getString(R.string.grant_sms_permission)
            setOnClickListener { requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 100) }
        })
        setContentView(root)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100) {
            val ok = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            Toast.makeText(
                this,
                if (ok) getString(R.string.permission_granted) else getString(R.string.permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
