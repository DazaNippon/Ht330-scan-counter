package com.example.scancounter

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvCount: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnReset: Button

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ScanCountService.ACTION_COUNT_UPDATED) {
                val count = intent.getIntExtra(ScanCountService.EXTRA_COUNT, 0)
                tvCount.text = count.toString()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCount = findViewById(R.id.tvCount)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnReset = findViewById(R.id.btnReset)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        btnStart.setOnClickListener {
            val intent = Intent(this, ScanCountService::class.java).apply {
                action = ScanCountService.ACTION_START
            }
            ContextCompat.startForegroundService(this, intent)
        }

        btnStop.setOnClickListener {
            val intent = Intent(this, ScanCountService::class.java).apply {
                action = ScanCountService.ACTION_STOP
            }
            startService(intent)
        }

        btnReset.setOnClickListener {
            val intent = Intent(this, ScanCountService::class.java).apply {
                action = ScanCountService.ACTION_RESET
            }
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        tvCount.text = ScanCountService.currentScanCount.toString()
        val filter = IntentFilter(ScanCountService.ACTION_COUNT_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(updateReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(updateReceiver)
        } catch (_: Exception) {}
    }
}
