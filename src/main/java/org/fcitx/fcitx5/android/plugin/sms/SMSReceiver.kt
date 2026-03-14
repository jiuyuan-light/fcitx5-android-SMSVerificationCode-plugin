package org.fcitx.fcitx5.android.plugin.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action || 
                Telephony.Sms.Intents.SMS_DELIVER_ACTION == intent.action) {
                
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
