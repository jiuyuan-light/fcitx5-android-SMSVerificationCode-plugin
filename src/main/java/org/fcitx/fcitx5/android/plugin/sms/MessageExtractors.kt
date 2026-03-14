package org.fcitx.fcitx5.android.plugin.sms

import android.app.Notification
import android.content.Intent
import android.provider.Telephony
import android.service.notification.StatusBarNotification

internal object MessageExtractors {
    fun extractSmsBodies(intent: Intent): List<String> {
        return Telephony.Sms.Intents.getMessagesFromIntent(intent)
            ?.mapNotNull { it.messageBody }
            ?: emptyList()
    }

    fun extractNotificationText(sbn: StatusBarNotification?): String? {
        val extras = sbn?.notification?.extras ?: return null
        val parts = ArrayList<CharSequence>(5)
        extras.getCharSequence(Notification.EXTRA_TITLE)?.let { parts.add(it) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it) }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let { parts.addAll(it) }
        val content = parts.joinToString(" ") { it.toString() }.trim()
        return content.ifEmpty { null }
    }
}
