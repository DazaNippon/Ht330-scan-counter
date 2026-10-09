package com.example.scancounter

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

class ScanAccessibilityService : AccessibilityService() {

    companion object {
        const val CHANNEL_ID = "scan_counter_channel"
        const val NOTIFICATION_ID = 101
        const val ACTION_RESET = "com.example.scancounter.ACTION_RESET"

        var scanCount = 0
            private set
        var instance: ScanAccessibilityService? = null
    }

    private var lastScanCountedTime = 0L

    // Catches scans if USS broadcasts them (e.g. in USS test screen)
    private val ussReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            registerScan()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        createNotificationChannel()
        updateNotification()

        val filter = IntentFilter("unitech.scanservice.data")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ussReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(ussReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESET) {
            resetCount()
        }
        return START_STICKY
    }

    // Catches barcodes inserted into GLOW fields via commitText, copy-paste, or auto-input
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val added = event.addedCount
            // When a barcode is scanned, multiple characters are inserted at once
            if (added >= 2) {
                registerScan()
            }
        }
    }

    // Catches keyevents if USS or scanner sends Enter/Tab
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isEnterOrTab = event.keyCode == KeyEvent.KEYCODE_ENTER || 
                               event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || 
                               event.keyCode == KeyEvent.KEYCODE_TAB

            if (isEnterOrTab) {
                registerScan()
            }
        }
        return false // Passes key event to GLOW untouched
    }

    @Synchronized
    private fun registerScan() {
        val now = System.currentTimeMillis()
        // Debounce: prevent multiple triggers from the same scan within 500ms
        if (now - lastScanCountedTime > 500) {
            lastScanCountedTime = now
            scanCount++
            updateNotification()
            sendBroadcast(Intent("com.example.scancounter.COUNT_UPDATED"))
        }
    }

    fun resetCount() {
        scanCount = 0
        updateNotification()
        sendBroadcast(Intent("com.example.scancounter.COUNT_UPDATED"))
    }

    private fun updateNotification() {
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = PendingIntent.getService(
            this, 1, Intent(this, ScanAccessibilityService::class.java).apply { action = ACTION_RESET },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Warehouse Scan Counter")
            .setContentText("Total Scans: $scanCount")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_revert, "Reset", resetIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scan Counter",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        try {
            unregisterReceiver(ussReceiver)
        } catch (_: Exception) {}
        instance = null
        super.onDestroy()
    }
}
