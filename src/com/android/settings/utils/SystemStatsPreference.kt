package com.android.settings.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.storage.StorageManager
import android.text.format.Formatter
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import com.android.settings.R
import com.android.settingslib.deviceinfo.PrivateStorageInfo
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider
import com.android.settingslib.widget.LayoutPreference

class SystemStatsPreference(context: Context, attrs: AttributeSet) : LayoutPreference(context, attrs) {
    companion object {
        private const val TAG = "SystemStatsPreference"
    }

    private lateinit var batteryCircle: CircularProgressView
    private lateinit var storageCircle: CircularProgressView
    private lateinit var batteryPercentage: TextView
    private lateinit var batteryTemp: TextView
    private lateinit var batteryRemaining: TextView
    private lateinit var storageAvailable: TextView
    private lateinit var storageTotal: TextView
    private lateinit var batteryTile: View
    private lateinit var storageTile: View

    private lateinit var batteryReceiver: BroadcastReceiver
    private val mStorageManager: StorageManager
    private val mStorageManagerVolumeProvider: StorageManagerVolumeProvider

    init {
        mStorageManager = context.getSystemService(StorageManager::class.java)
        mStorageManagerVolumeProvider = StorageManagerVolumeProvider(mStorageManager)
        initViews()
    }

    private fun initViews() {
        batteryCircle = findViewById(R.id.battery_circle)
        storageCircle = findViewById(R.id.storage_circle)
        batteryPercentage = findViewById(R.id.battery_percentage)
        batteryTemp = findViewById(R.id.battery_temp)
        batteryRemaining = findViewById(R.id.battery_remaining)
        storageAvailable = findViewById(R.id.storage_available)
        storageTotal = findViewById(R.id.storage_total)
        batteryTile = findViewById(R.id.battery_tile)
        storageTile = findViewById(R.id.storage_tile)

        setupClickListeners()
        setupBatteryReceiver()
        updateStorageInfo()
    }

    private fun setupClickListeners() {
        batteryTile.setOnClickListener {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            context.startActivity(intent)
        }

        storageTile.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            context.startActivity(intent)
        }
    }

    private fun setupBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                    updateBatteryInfo(intent, context)
                }
            }
        }
    }

    private fun updateBatteryInfo(intent: Intent, context: Context) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val batteryPct = level * 100 / scale.toFloat()

        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL

        batteryCircle.setProgress(batteryPct)
        batteryPercentage.text = String.format("%d%%", Math.round(batteryPct))
        batteryTemp.text = String.format("%.1f°C", temp)

        if (isFull) {
            batteryRemaining.text = context.getString(R.string.battery_fully_charged)
        } else if (isCharging) {
            batteryRemaining.text = context.getString(R.string.battery_charging)
        } else {
            batteryRemaining.text = context.getString(R.string.battery_discharging)
        }
    }

    private fun updateStorageInfo() {
        try {
            val info = PrivateStorageInfo.getPrivateStorageInfo(mStorageManagerVolumeProvider)

            val totalBytes = info.totalBytes
            val freeBytes = info.freeBytes
            val usedBytes = totalBytes - freeBytes

            val percentageUsed = (usedBytes.toFloat() / totalBytes) * 100

            val formattedUsed = Formatter.formatFileSize(context, usedBytes)
            val formattedAvailable = Formatter.formatFileSize(context, freeBytes)
            val formattedTotal = Formatter.formatFileSize(context, totalBytes)
            val storageUsed = context.getString(R.string.storage_card_used)

            storageCircle.setProgress(percentageUsed)
            storageAvailable.text = formattedAvailable
            storageTotal.text = String.format("%s / %s %s", formattedUsed, formattedTotal, storageUsed)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating storage info", e)
            storageCircle.setProgress(0f)
            storageAvailable.text = "-- GB"
            storageTotal.text = "-- GB"
        }
    }

    override fun onAttached() {
        super.onAttached()
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onDetached() {
        super.onDetached()
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Battery receiver not registered", e)
        }
    }
}