package com.blocksilent.app.utils;

import android.content.Context;
import android.util.Log;

import com.blocksilent.app.context.ContextEngine;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.ActiveGeofenceStateEntity;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.WifiZoneEntity;
import com.blocksilent.app.emergency.EmergencyManager;
import com.blocksilent.app.geofence.GeofenceEventProcessor;
import com.blocksilent.app.wearable.WearableNotificationHelper;

public class DemoModeManager {
    private static final String TAG = "BLOCKSILENT_DEMO";
    private static String currentDemoBlockName = "Outside Configured Area";
    private static long currentDemoBlockId = -1;
    private static boolean isDemoModeActive = false;

    private final Context context;
    private final GeofenceEventProcessor eventProcessor;
    private final ContextEngine contextEngine;
    private final EmergencyManager emergencyManager;
    private final WearableNotificationHelper wearableHelper;
    private final AppDatabase database;

    public DemoModeManager(Context context) {
        this.context = context.getApplicationContext();
        this.eventProcessor = new GeofenceEventProcessor(this.context);
        this.contextEngine = ContextEngine.getInstance(this.context);
        this.emergencyManager = new EmergencyManager(this.context);
        this.wearableHelper = new WearableNotificationHelper(this.context);
        this.database = AppDatabase.getInstance(this.context);
    }

    public static boolean isDemoModeActive() {
        return isDemoModeActive;
    }

    public static String getCurrentDemoBlockName() {
        return currentDemoBlockName;
    }

    public static long getCurrentDemoBlockId() {
        return currentDemoBlockId;
    }

    /**
     * Simulate entering a specific block dynamically.
     */
    public void simulateEnterBlock(long blockId) {
        isDemoModeActive = true;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            BlockEntity targetBlock = database.blockDao().getBlockById(blockId);
            if (targetBlock != null) {
                currentDemoBlockName = targetBlock.getName();
                currentDemoBlockId = targetBlock.getId();
                Log.d(TAG, "DEMO ENTER: " + targetBlock.getName() + " (ID: " + blockId + ")");
                eventProcessor.processDemoEvent(targetBlock.getId(), "ENTER");
            }
        });
    }

    /**
     * Simulate exiting the current block.
     */
    public void simulateExitBlock() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long blockId = currentDemoBlockId > 0 ? currentDemoBlockId : 1;
            Log.d(TAG, "DEMO EXIT: " + currentDemoBlockName + " (ID: " + blockId + ")");
            currentDemoBlockName = "Outside Configured Area";
            currentDemoBlockId = -1;
            eventProcessor.processDemoEvent(blockId, "EXIT");
        });
    }

    /**
     * Simulate Wi-Fi BSSID Room Detection.
     */
    public void simulateWifiRoomDetection(String roomName, String bssid, String policy) {
        isDemoModeActive = true;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Log.d(TAG, "DEMO Wi-Fi Room: " + roomName + " [" + bssid + "] Policy: " + policy);
            // Insert or ensure demo zone exists
            WifiZoneEntity zone = database.wifiZoneDao().findByBssidSync(bssid);
            if (zone == null) {
                zone = new WifiZoneEntity(bssid, "Campus-WiFi-Demo", -55, roomName, "2", 1, "Academic Block A", policy, true, System.currentTimeMillis());
                database.wifiZoneDao().insertOrUpdate(zone);
            }
            contextEngine.triggerContextEvaluation("DEMO_WIFI");
        });
    }

    /**
     * Simulate Sensor Flip-to-Silence.
     */
    public void simulateFlipToSilence(boolean faceDown) {
        isDemoModeActive = true;
        Log.d(TAG, "DEMO Flip-to-Silence: " + (faceDown ? "FACE-DOWN" : "PICKED UP"));
        contextEngine.updateSensorSilenceState(faceDown);
    }

    /**
     * Simulate VIP Emergency SMS Break-Glass safely in sandbox.
     */
    public void simulateEmergencyKeywordBreakGlass() {
        isDemoModeActive = true;
        Log.d(TAG, "DEMO VIP Emergency Break-Glass triggered in sandbox.");
        emergencyManager.triggerEmergencyOverride("+19998887777", "DEMO VIP SMS: EMERGENCY", true);
    }

    /**
     * Simulate Campus Noise Level.
     */
    public void simulateNoiseSample(double noiseDb) {
        isDemoModeActive = true;
        Log.d(TAG, "DEMO Noise Level: " + noiseDb + " dB");
        contextEngine.updateNoiseSample(noiseDb);
        contextEngine.triggerContextEvaluation("DEMO_NOISE");
    }

    /**
     * Simulate Wearable Haptic Notification test.
     */
    public void simulateWearableNotification() {
        wearableHelper.notifyZoneTransition(
                "Wearable Haptic Test",
                "Synchronized haptic pulse received.",
                "ENTER"
        );
    }
}
