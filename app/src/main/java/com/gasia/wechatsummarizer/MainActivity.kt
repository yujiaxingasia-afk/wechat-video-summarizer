package com.gasia.wechatsummarizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnStartCapture: Button
    private lateinit var btnStopCapture: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        btnStartCapture = findViewById(R.id.btnStartCapture)
        btnStopCapture = findViewById(R.id.btnStopCapture)

        setupButtons()
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun setupButtons() {
        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnStartCapture.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            WeChatVideoService.start()
            Toast.makeText(this, "已开始捕获，请去微信看视频", Toast.LENGTH_SHORT).show()
            updateStatus()
        }

        btnStopCapture.setOnClickListener {
            WeChatVideoService.stop()
            Toast.makeText(this, "已停止捕获", Toast.LENGTH_SHORT).show()
            updateStatus()
        }
    }

    private fun updateStatus() {
        val enabled = isAccessibilityEnabled()
        statusText.text = if (enabled) {
            "✅ 无障碍服务已开启"
        } else {
            "❌ 无障碍服务未开启"
        }
        
        btnStartCapture.isEnabled = enabled
        btnStopCapture.isEnabled = enabled && WeChatVideoService.isServiceRunning()
    }

    private fun isAccessibilityEnabled(): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                return settingValue.contains(packageName)
            }
        }
        return false
    }
}