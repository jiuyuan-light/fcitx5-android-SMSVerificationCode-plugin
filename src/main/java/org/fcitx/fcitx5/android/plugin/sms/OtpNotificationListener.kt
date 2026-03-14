package org.fcitx.fcitx5.android.plugin.sms

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

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
