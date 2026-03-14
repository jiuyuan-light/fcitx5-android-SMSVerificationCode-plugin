package org.fcitx.fcitx5.android.plugin.sms

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger

/**
 * Empty service for Fcitx5-Android to bind to.
 */
class MainService : Service() {
    override fun onBind(intent: Intent): IBinder = Messenger(Handler(Looper.getMainLooper())).binder
}
