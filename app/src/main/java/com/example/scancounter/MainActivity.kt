package com.example.scancounter

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var countText: TextView
    private lateinit var resetButton: Button
    private lateinit var enableServiceButton: Button

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        countText = findViewById(R.id.countText)
        resetButton = findViewById(R.id.resetButton)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        resetButton.setOnClickListener {
            ScanAccessibilityService.instance?.resetCount()
            updateDisplay()
        }

        // Button to open Accessibility Settings if not yet turned on
        val btnAccessibility = Button(this).apply {
            text = "Enable in Accessibility Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        (countText.parent as? android.view.ViewGroup)?.addView(btnAccessibility)

        updateDisplay()
    }

    private fun updateDisplay() {
        countText.text = "${ScanAccessibilityService.scanCount}"
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
