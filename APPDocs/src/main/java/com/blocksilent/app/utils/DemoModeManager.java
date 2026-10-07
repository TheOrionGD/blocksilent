package com.blocksilent.app.utils;

import android.content.Context;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.geofence.GeofenceEventProcessor;

public class DemoModeManager {
    private static final String TAG = "BLOCKSILENT_GEOFENCE";
    private static String currentDemoBlockName = "Outside Configured Area";
    private static long currentDemoBlockId = -1;

    private final Context context;
    private final GeofenceEventProcessor eventProcessor;

    public DemoModeManager(Context context) {
        this.context = context.getApplicationContext();
        this.eventProcessor = new GeofenceEventProcessor(this.context);
    }

    public static String getCurrentDemoBlockName() {
        return currentDemoBlockName;
    }

    public static long getCurrentDemoBlockId() {
        return currentDemoBlockId;
    }

    /**
     * Simulate entering a specific block dynamically by ID from database.
     */
    public void simulateEnterBlock(long blockId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            BlockEntity targetBlock = AppDatabase.getInstance(context).blockDao().getBlockById(blockId);
            if (targetBlock != null) {
                currentDemoBlockName = targetBlock.getName();
                currentDemoBlockId = targetBlock.getId();
                Log.d(TAG, "BLOCKSILENT_GEOFENCE: DEMO ENTER invoked for " + targetBlock.getName() + " (ID: " + blockId + ")");
                eventProcessor.processDemoEvent(targetBlock.getId(), "ENTER");
            } else {
                Log.w(TAG, "BLOCKSILENT_GEOFENCE: DEMO target block ID not found: " + blockId);
            }
        });
    }

    /**
     * Simulate exiting the current block.
     */
    public void simulateExitBlock() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long blockId = currentDemoBlockId > 0 ? currentDemoBlockId : 1;
            Log.d(TAG, "BLOCKSILENT_GEOFENCE: DEMO EXIT invoked for " + currentDemoBlockName + " (ID: " + blockId + ")");
            currentDemoBlockName = "Outside Configured Area";
            currentDemoBlockId = -1;
            eventProcessor.processDemoEvent(blockId, "EXIT");
        });
    }
}
