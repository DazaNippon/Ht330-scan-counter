package com.example.scancounter

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

    private var charCountInBurst = 0
    private var lastKeyTime = 0L
    private var lastScanCountedTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        createNotificationChannel()
        updateNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESET) {
            resetCount()
        }
        return START_STICKY
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            val timeSinceLastKey = now - lastKeyTime
            lastKeyTime = now

            // Barcode scanners type characters with near-zero delay (< 80ms per key)
            if (timeSinceLastKey < 100) {
                charCountInBurst++
            } else {
                charCountInBurst = 1
            }

            val isEnterOrTab = event.keyCode == KeyEvent.KEYCODE_ENTER || 
                               event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || 
                               event.keyCode == KeyEvent.KEYCODE_TAB

            // If an Enter/Tab terminates a rapid character sequence, or 5+ rapid chars arrive
            if (isEnterOrTab && charCountInBurst >= 2) {
                if (now - lastScanCountedTime > 400) { // Debounce 400ms
                    registerScan()
                    lastScanCountedTime = now
                }
                charCountInBurst = 0
            }
        }

        // Return false so the key event passes directly to GLOW untouched!
        return false
    }

    private fun registerScan() {
        scanCount++
        updateNotification()
        sendBroadcast(Intent("com.example.scancounter.COUNT_UPDATED"))
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
