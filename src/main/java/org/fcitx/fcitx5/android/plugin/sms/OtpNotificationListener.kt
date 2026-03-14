package org.fcitx.fcitx5.android.plugin.sms

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class OtpNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            super.onNotificationPosted(sbn)
            
            if (sbn == null) return
            
            val notification = sbn.notification
            val extras = notification.extras ?: return
            
            val title = extras.getString("android.title")
            val text = extras.getString("android.text")
            val bigText = extras.getString("android.bigText")
            
            val content = "${title ?: ""} ${text ?: ""} ${bigText ?: ""}"
            
            if (content.isBlank()) return
    
            val code = VerificationCodeExtractor.extract(content)
            if (code != null) {
                ClipboardUtil.copy(this, code)
            }
        } catch (t: Throwable) {
            Log.e("Fcitx5SmsPlugin", "Error in onNotificationPosted", t)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("Fcitx5SmsPlugin", "Notification Listener Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i("Fcitx5SmsPlugin", "Notification Listener Disconnected")
    }
}
