package com.blocksilent.app.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.geofence.GeofenceManager;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device boot completed. Re-registering geofences...");
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase database = AppDatabase.getInstance(context);
                SettingsEntity settings = database.settingsDao().getSettingsSync();

                if (settings == null || !settings.isAutomationEnabled()) {
                    Log.d(TAG, "Automation disabled; skipping boot geofence registration.");
                    return;
                }

                if (settings.getOverrideUntilTimestamp() > System.currentTimeMillis()) {
                    Log.d(TAG, "Emergency override active; skipping boot geofence registration.");
                    return;
                }

                List<BlockEntity> enabledBlocks = database.blockDao().getEnabledBlocksSync();
                GeofenceManager geofenceManager = new GeofenceManager(context);
                geofenceManager.registerGeofences(enabledBlocks);
            });
        }
    }
}
