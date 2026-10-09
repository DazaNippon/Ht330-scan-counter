package com.example.scancounter

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var tvCount: TextView
    private lateinit var btnReset: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCount = findViewById(R.id.tvCount)
        btnReset = findViewById(R.id.btnReset)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        // Tap to open Android Accessibility Settings to turn the service ON
        btnStart.text = "Enable Accessibility Service"
        btnStart.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnReset.setOnClickListener {
            ScanAccessibilityService.instance?.resetCount()
            updateDisplay()
        }

        // Hide unused stop button (accessibility services are toggled in system settings)
        btnStop.visibility = View.GONE

        updateDisplay()
    }

    private fun updateDisplay() {
        tvCount.text = "${ScanAccessibilityService.scanCount}"
    }

    override fun onResume() {
        super.onResume()
        updateDisplay()
        val filter = IntentFilter("com.example.scancounter.COUNT_UPDATED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
}
