package com.blocksilent.app.utils;

import android.content.Context;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.ActiveGeofenceStateEntity;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.HistoryEntity;
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

    public RuleEngine(Context context) {
        this.context = context.getApplicationContext();
        this.soundModeManager = new SoundModeManager(this.context);
        this.notificationHelper = new NotificationHelper(this.context);
        this.database = AppDatabase.getInstance(this.context);
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

        if (settings != null && settings.getOverrideUntilTimestamp() > System.currentTimeMillis()) {
            Log.d(TAG, "BLOCKSILENT_RULE: Emergency override active until " + settings.getOverrideUntilTimestamp() + ". Skipping " + transitionType);
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

        // 1. Capture current ringer mode before any automation change
        String currentMode = soundModeManager.getCurrentRingerMode();
        soundModeManager.savePreviousMode();
        String savedPrevMode = soundModeManager.getSavedPreviousSoundMode();

        // 2. Update active geofence state in Room
        ActiveGeofenceStateEntity existingState = database.activeGeofenceStateDao().getStateByBlockIdSync(blockId);
        String previousModeForBlock = (existingState != null && existingState.getPreviousSoundMode() != null)
                ? existingState.getPreviousSoundMode() : currentMode;

        // 3. Resolve dominant block among all active overlapping blocks
        List<ActiveGeofenceStateEntity> insideStates = database.activeGeofenceStateDao().getInsideStatesSync();
        BlockEntity dominantBlock = block;
        for (ActiveGeofenceStateEntity state : insideStates) {
            if (state.getBlockId() != blockId) {
                BlockEntity otherBlock = database.blockDao().getBlockById(state.getBlockId());
                if (otherBlock != null && otherBlock.isEnabled() && otherBlock.getPriority() < dominantBlock.getPriority()) {
                    dominantBlock = otherBlock;
                }
            }
        }

        // 4. Determine target sound mode via Timetable and Priority
        String targetMode = evaluateTargetSoundMode(dominantBlock);
        Log.d(TAG, "BLOCKSILENT_RULE: ENTER " + block.getName() + " (Priority=" + block.getPriority() + "). Dominant: " + dominantBlock.getName() + " -> TargetMode=" + targetMode);

        // 5. Apply sound mode and verify result
        SoundModeManager.SoundChangeResult result = soundModeManager.applySoundModeWithResult(targetMode);

        // 6. Persist state in active_geofence_state
        ActiveGeofenceStateEntity stateEntity = new ActiveGeofenceStateEntity(
                blockId,
                block.getName(),
                "INSIDE",
                previousModeForBlock,
                result.actualMode,
                now,
                transitionType,
                now
        );
        database.activeGeofenceStateDao().insertOrUpdate(stateEntity);

        // 7. Insert History record
        String formattedTime = getFormattedCurrentTime();
        HistoryEntity history = new HistoryEntity(
                now,
                formattedTime,
                block.getName(),
                "Entered " + block.getName() + (result.success ? "" : " (Sound Policy Warning)"),
                currentMode,
                result.actualMode
        );
        database.historyDao().insert(history);

        // 8. Trigger user notification
        if (settings == null || settings.isNotificationsEnabled()) {
            if (result.success) {
                notificationHelper.showEnterBlockNotification(block.getName(), currentMode, result.actualMode);
            } else {
                notificationHelper.showSoundChangeFailedNotification(result.failureReason);
            }
        }
    }

    private void handleExit(BlockEntity block, SettingsEntity settings) {
        long blockId = block.getId();
        long now = System.currentTimeMillis();

        // 1. Mark this block as OUTSIDE in state table
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

        // 2. Check if any OTHER blocks are still active (overlapping geofences)
        List<ActiveGeofenceStateEntity> remainingInside = database.activeGeofenceStateDao().getInsideStatesSync();
        String currentMode = soundModeManager.getCurrentRingerMode();

        if (remainingInside != null && !remainingInside.isEmpty()) {
            // Still inside one or more overlapping geofences: find highest priority remaining
            BlockEntity highestPriorityBlock = null;
            for (ActiveGeofenceStateEntity state : remainingInside) {
                BlockEntity b = database.blockDao().getBlockById(state.getBlockId());
                if (b != null && b.isEnabled()) {
                    if (highestPriorityBlock == null || b.getPriority() < highestPriorityBlock.getPriority()) {
                        highestPriorityBlock = b;
                    }
                }
            }

            if (highestPriorityBlock != null) {
                String targetMode = evaluateTargetSoundMode(highestPriorityBlock);
                Log.d(TAG, "BLOCKSILENT_RULE: Exited " + block.getName() + " but still inside " + highestPriorityBlock.getName() + ". Applying " + targetMode);
                SoundModeManager.SoundChangeResult res = soundModeManager.applySoundModeWithResult(targetMode);

                HistoryEntity history = new HistoryEntity(
                        now,
                        getFormattedCurrentTime(),
                        block.getName(),
                        "Exited " + block.getName() + " (In " + highestPriorityBlock.getName() + ")",
                        currentMode,
                        res.actualMode
                );
                database.historyDao().insert(history);

                if (settings == null || settings.isNotificationsEnabled()) {
                    notificationHelper.showExitBlockNotification(block.getName(), "Maintained for " + highestPriorityBlock.getName());
                }
                return;
            }
        }

        // 3. User is outside ALL configured blocks -> Restore original sound mode
        Log.d(TAG, "BLOCKSILENT_RULE: Outside all configured blocks. Restoring original sound mode.");
        SoundModeManager.SoundChangeResult restoreResult = soundModeManager.restorePreviousMode();

        HistoryEntity history = new HistoryEntity(
                now,
                getFormattedCurrentTime(),
                block.getName(),
                "Exited " + block.getName(),
                currentMode,
                restoreResult.actualMode
        );
        database.historyDao().insert(history);

        if (settings == null || settings.isNotificationsEnabled()) {
            notificationHelper.showExitBlockNotification(block.getName(), restoreResult.actualMode);
        }
    }

    /**
     * Check if a Timetable entry overrides the block's sound mode for the current time.
     */
    public String evaluateTargetSoundMode(BlockEntity block) {
        String currentDay = getCurrentDayOfWeek();
        String currentTime = getCurrentTimeHHmm();

        List<TimetableEntity> scheduleList = database.timetableDao().getEntriesForBlockSync(block.getId());
        if (scheduleList != null) {
            for (TimetableEntity item : scheduleList) {
                if (item.isEnabled() && item.getDayOfWeek().equalsIgnoreCase(currentDay)) {
                    if (isTimeBetween(currentTime, item.getStartTime(), item.getEndTime())) {
                        Log.d(TAG, "BLOCKSILENT_RULE: Timetable match: " + item.getSubjectName() + " (" + item.getSoundMode() + ")");
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

    private String getFormattedCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void processOutsideAllBlocks() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ActiveGeofenceStateEntity> inside = database.activeGeofenceStateDao().getInsideStatesSync();
            if (inside == null || inside.isEmpty()) {
                soundModeManager.restorePreviousMode();
            }
        });
    }
}
