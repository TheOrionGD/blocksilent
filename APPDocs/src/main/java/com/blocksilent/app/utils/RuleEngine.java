package com.blocksilent.app.utils;

import android.content.Context;
import android.util.Log;

import com.blocksilent.app.context.ContextEngine;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.ActiveGeofenceStateEntity;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.database.entities.TimetableEntity;
import com.blocksilent.app.notifications.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RuleEngine {
    private static final String TAG = "BLOCKSILENT_RULE";

    private final Context context;
    private final SoundModeManager soundModeManager;
    private final NotificationHelper notificationHelper;
    private final AppDatabase database;
    private final ContextEngine contextEngine;

    public RuleEngine(Context context) {
        this.context = context.getApplicationContext();
        this.soundModeManager = new SoundModeManager(this.context);
        this.notificationHelper = new NotificationHelper(this.context);
        this.database = AppDatabase.getInstance(this.context);
        this.contextEngine = ContextEngine.getInstance(this.context);
    }

    /**
     * Process an ENTER / DWELL geofence event.
     */
    public void processBlockEntry(long blockId) {
        processBlockTransition(blockId, "ENTER");
    }

    /**
     * Process an EXIT geofence event.
     */
    public void processBlockExit(long blockId) {
        processBlockTransition(blockId, "EXIT");
    }

    /**
     * Core Rule Engine execution for ENTER, DWELL, and EXIT transitions.
     */
    public void processBlockTransition(long blockId, String transitionType) {
        SettingsEntity settings = database.settingsDao().getSettingsSync();
        if (settings != null && !settings.isAutomationEnabled()) {
            Log.d(TAG, "BLOCKSILENT_RULE: Automation is disabled in settings. Skipping " + transitionType + " for block " + blockId);
            return;
        }

        BlockEntity block = database.blockDao().getBlockById(blockId);
        if (block == null) {
            Log.w(TAG, "BLOCKSILENT_RULE: Block id " + blockId + " not found in database");
            return;
        }

        if (!block.isEnabled()) {
            Log.d(TAG, "BLOCKSILENT_RULE: Block " + block.getName() + " is disabled. Skipping");
            return;
        }

        if ("ENTER".equalsIgnoreCase(transitionType) || "DWELL".equalsIgnoreCase(transitionType)) {
            handleEnterOrDwell(block, transitionType, settings);
        } else if ("EXIT".equalsIgnoreCase(transitionType)) {
            handleExit(block, settings);
        }
    }

    private void handleEnterOrDwell(BlockEntity block, String transitionType, SettingsEntity settings) {
        long blockId = block.getId();
        long now = System.currentTimeMillis();

        String currentMode = soundModeManager.getCurrentRingerMode();
        ActiveGeofenceStateEntity existingState = database.activeGeofenceStateDao().getStateByBlockIdSync(blockId);
        String previousModeForBlock = (existingState != null && existingState.getPreviousSoundMode() != null)
                ? existingState.getPreviousSoundMode() : currentMode;

        // Persist state in active_geofence_state
        ActiveGeofenceStateEntity stateEntity = new ActiveGeofenceStateEntity(
                blockId,
                block.getName(),
                "INSIDE",
                previousModeForBlock,
                currentMode,
                now,
                transitionType,
                now
        );
        database.activeGeofenceStateDao().insertOrUpdate(stateEntity);

        // Execute unified multi-signal context engine arbitration
        contextEngine.evaluateAndApplyContext("GEOFENCE_ENTER");
    }

    private void handleExit(BlockEntity block, SettingsEntity settings) {
        long blockId = block.getId();
        long now = System.currentTimeMillis();

        ActiveGeofenceStateEntity existingState = database.activeGeofenceStateDao().getStateByBlockIdSync(blockId);
        String previousModeForBlock = existingState != null ? existingState.getPreviousSoundMode() : SoundModeManager.MODE_NORMAL;

        ActiveGeofenceStateEntity updatedState = new ActiveGeofenceStateEntity(
                blockId,
                block.getName(),
                "OUTSIDE",
                previousModeForBlock,
                soundModeManager.getCurrentRingerMode(),
                0,
                "EXIT",
                now
        );
        database.activeGeofenceStateDao().insertOrUpdate(updatedState);

        // Re-evaluate context engine to handle overlapping zones or auto-restore
        contextEngine.evaluateAndApplyContext("GEOFENCE_EXIT");
    }

    public String evaluateTargetSoundMode(BlockEntity block) {
        String currentDay = getCurrentDayOfWeek();
        String currentTime = getCurrentTimeHHmm();

        List<TimetableEntity> scheduleList = database.timetableDao().getEntriesForBlockSync(block.getId());
        if (scheduleList != null) {
            for (TimetableEntity item : scheduleList) {
                if (item.isEnabled() && item.getDayOfWeek().equalsIgnoreCase(currentDay)) {
                    if (isTimeBetween(currentTime, item.getStartTime(), item.getEndTime())) {
                        return item.getSoundMode();
                    }
                }
            }
        }
        return block.getSoundMode();
    }

    private boolean isTimeBetween(String targetTime, String startTime, String endTime) {
        try {
            return targetTime.compareTo(startTime) >= 0 && targetTime.compareTo(endTime) <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String getCurrentDayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
    }

    private String getCurrentTimeHHmm() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void processOutsideAllBlocks() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ActiveGeofenceStateEntity> inside = database.activeGeofenceStateDao().getInsideStatesSync();
            if (inside == null || inside.isEmpty()) {
                contextEngine.evaluateAndApplyContext("OUTSIDE_ALL");
            }
        });
    }
}
