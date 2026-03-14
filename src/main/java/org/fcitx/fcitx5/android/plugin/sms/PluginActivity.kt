package org.fcitx.fcitx5.android.plugin.sms

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class PluginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
