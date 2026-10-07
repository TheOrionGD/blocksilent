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

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BLOCKSILENT_BOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "BLOCKSILENT_BOOT: Received action=" + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Log.d(TAG, "BLOCKSILENT_BOOT: Device boot or package replaced. Checking permissions and restoring geofences...");

            if (!PermissionManager.hasLocationPermission(context)) {
                Log.w(TAG, "BLOCKSILENT_BOOT: Location permissions missing upon boot. Cannot re-register geofences.");
                return;
            }

            final PendingResult pendingResult = goAsync();

            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    AppDatabase database = AppDatabase.getInstance(context);
                    SettingsEntity settings = database.settingsDao().getSettingsSync();

                    if (settings == null || !settings.isAutomationEnabled()) {
                        Log.d(TAG, "BLOCKSILENT_BOOT: Automation disabled in settings. Skipping geofence registration.");
                        return;
                    }

                    List<BlockEntity> enabledBlocks = database.blockDao().getEnabledBlocksSync();
                    if (enabledBlocks != null && !enabledBlocks.isEmpty()) {
                        GeofenceManager geofenceManager = new GeofenceManager(context);
                        geofenceManager.registerGeofences(enabledBlocks);
                        Log.d(TAG, "BLOCKSILENT_BOOT: Restored " + enabledBlocks.size() + " geofence(s) after reboot.");
                    } else {
                        Log.d(TAG, "BLOCKSILENT_BOOT: No enabled blocks to restore.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "BLOCKSILENT_BOOT: Error restoring geofences on boot", e);
                } finally {
                    try {
                        pendingResult.finish();
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }
}
