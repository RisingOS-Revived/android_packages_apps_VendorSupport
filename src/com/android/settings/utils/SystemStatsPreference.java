package com.android.settings.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.storage.StorageManager;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.widget.LayoutPreference;

import java.io.File;

public class SystemStatsPreference extends LayoutPreference {
    private static final String TAG = "SystemStatsPreference";

    private CircularProgressView batteryCircle;
    private CircularProgressView storageCircle;
    private TextView batteryPercentage;
    private TextView batteryTemp;
    private TextView batteryRemaining;
    private TextView storageAvailable;
    private TextView storageTotal;
    private View batteryTile;
    private View storageTile;

    private BroadcastReceiver batteryReceiver;
    private final StorageManager mStorageManager;
    private final StorageManagerVolumeProvider mStorageManagerVolumeProvider;

    public SystemStatsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mStorageManager = context.getSystemService(StorageManager.class);
        mStorageManagerVolumeProvider = new StorageManagerVolumeProvider(mStorageManager);
        initViews();
    }

    private void initViews() {
        batteryCircle = findViewById(R.id.battery_circle);
        storageCircle = findViewById(R.id.storage_circle);
        batteryPercentage = findViewById(R.id.battery_percentage);
        batteryTemp = findViewById(R.id.battery_temp);
        batteryRemaining = findViewById(R.id.battery_remaining);
        storageAvailable = findViewById(R.id.storage_available);
        storageTotal = findViewById(R.id.storage_total);
        batteryTile = findViewById(R.id.battery_tile);
        storageTile = findViewById(R.id.storage_tile);

        setupClickListeners();
        setupBatteryReceiver();
        updateStorageInfo();
    }

    private void setupClickListeners() {
        batteryTile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
            getContext().startActivity(intent);
        });

        storageTile.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
            getContext().startActivity(intent);
        });
    }

    private void setupBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                    updateBatteryInfo(intent, context);
                }
            }
        };
    }

    private void updateBatteryInfo(Intent intent, Context context) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        float batteryPct = level * 100 / (float) scale;

        float temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;
        boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;

        batteryCircle.setProgress(batteryPct);
        batteryPercentage.setText(String.format("%d%%", Math.round(batteryPct)));
        batteryTemp.setText(String.format("%.1f°C", temp));

        if (isFull) {
            batteryRemaining.setText(getContext().getString(R.string.battery_fully_charged));
        } else if (isCharging) {
            batteryRemaining.setText(getContext().getString(R.string.battery_charging));
        } else {
            batteryRemaining.setText(getContext().getString(R.string.battery_discharging));
        }
    }

    private void updateStorageInfo() {
        try {
            PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(mStorageManagerVolumeProvider);

            long totalBytes = info.totalBytes;
            long freeBytes = info.freeBytes;
            long usedBytes = totalBytes - freeBytes;

            float percentageUsed = ((float) usedBytes / totalBytes) * 100;

            String formattedUsed = Formatter.formatFileSize(getContext(), usedBytes);
            String formattedAvailable = Formatter.formatFileSize(getContext(), freeBytes);
            String formattedTotal = Formatter.formatFileSize(getContext(), totalBytes);
            String storageUsed = getContext().getString(R.string.storage_card_used);

            storageCircle.setProgress(percentageUsed);
            storageAvailable.setText(formattedAvailable);
            storageTotal.setText(String.format("%s / %s %s", formattedUsed, formattedTotal, storageUsed));
        } catch (Exception e) {
            Log.e(TAG, "Error calculating storage info", e);
            storageCircle.setProgress(0);
            storageAvailable.setText("-- GB");
            storageTotal.setText("-- GB");
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();
        getContext().registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onDetached() {
        super.onDetached();
        try {
            getContext().unregisterReceiver(batteryReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Battery receiver not registered", e);
        }
    }
}
