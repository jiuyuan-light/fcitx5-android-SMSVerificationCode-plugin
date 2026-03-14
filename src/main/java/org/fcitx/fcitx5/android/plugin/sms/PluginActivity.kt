package org.fcitx.fcitx5.android.plugin.sms

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class PluginActivity : Activity() {

    private data class PluginInfo(
        val apiVersion: String?,
        val domain: String?,
        val description: String?,
        val hasService: Boolean?
    )

    private fun readPluginInfo(): PluginInfo {
        var apiVersion: String? = null
        var domain: String? = null
        var description: String? = null
        var hasService: Boolean? = null

        val parser = resources.getXml(R.xml.plugin)
        while (true) {
            when (parser.eventType) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "apiVersion" -> apiVersion = parser.nextText()
                        "domain" -> domain = parser.nextText()
                        "description" -> description = parser.nextText()
                        "hasService" -> hasService = parser.nextText().trim().equals("true", true)
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_DOCUMENT -> break
            }
            parser.next()
        }
        return PluginInfo(apiVersion, domain, description, hasService)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pluginInfo = readPluginInfo()

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        val infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.WHITE)
            elevation = 2f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 48)
            layoutParams = params
        }

        fun addInfoRow(label: String, value: String, multiline: Boolean = false) {
            val row = LinearLayout(this).apply {
                orientation = if (multiline) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            val labelTv = TextView(this).apply {
                text = if (multiline) label else "$label: "
                setTextColor(Color.GRAY)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            val valueTv = TextView(this).apply {
                text = value
                setTextColor(Color.BLACK)
                textSize = 14f
                if (multiline) {
                    setLineSpacing(8f, 1.15f)
                    setPadding(0, 8, 0, 0)
                }
            }
            row.addView(labelTv)
            row.addView(valueTv)
            infoCard.addView(row)
        }

        addInfoRow("包名", packageName)
        addInfoRow("插件 API 版本", pluginInfo.apiVersion ?: "-")
        addInfoRow("Gettext 域", pluginInfo.domain ?: "-")
        addInfoRow("插件描述", pluginInfo.description ?: "-", multiline = true)
        addInfoRow("包含跨进程通信服务", if (pluginInfo.hasService == true) "是" else "否")

        rootLayout.addView(infoCard)

        val btnSms = Button(this).apply {
            text = "授予短信权限"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
            setOnClickListener {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.RECEIVE_SMS
                    ),
                    100
                )
            }
        }
        rootLayout.addView(btnSms)

        val btnNotification = Button(this).apply {
            text = "授予通知监听权限"
            setBackgroundColor(Color.parseColor("#607D8B"))
            setTextColor(Color.WHITE)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = params
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
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
