package com.blocksilent.app.context;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.ActiveGeofenceStateEntity;
import com.blocksilent.app.database.entities.AutomationDecisionEntity;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.HistoryEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.database.entities.TimetableEntity;
import com.blocksilent.app.notifications.NotificationHelper;
import com.blocksilent.app.utils.SoundModeManager;
import com.blocksilent.app.utils.TimeUtils;
import com.blocksilent.app.wearable.WearableNotificationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContextEngine {
    private static final String TAG = "BLOCKSILENT_CONTEXT";
    private static volatile ContextEngine INSTANCE;

    private final Context context;
    private final AppDatabase database;
    private final SoundModeManager soundModeManager;
    private final NotificationHelper notificationHelper;
    private final WearableNotificationHelper wearableHelper;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<ContextState> liveContextState = new MutableLiveData<>();
    private boolean isEmergencyActive = false;

    public static ContextEngine getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ContextEngine.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ContextEngine(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private ContextEngine(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.context);
        this.soundModeManager = new SoundModeManager(this.context);
        this.notificationHelper = new NotificationHelper(this.context);
        this.wearableHelper = new WearableNotificationHelper(this.context);
    }

    public LiveData<ContextState> getLiveContextState() {
        return liveContextState;
    }

    public void updateEmergencyState(boolean emergency) {
        this.isEmergencyActive = emergency;
        evaluateAndApplyContext("EMERGENCY");
    }

    public void triggerContextEvaluation(String triggerSource) {
        evaluateAndApplyContext(triggerSource);
    }

    /**
     * Core multi-factor evaluation pipeline.
     */
    public synchronized void evaluateAndApplyContext(String triggerSource) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            SettingsEntity settings = database.settingsDao().getSettingsSync();

            boolean automationEnabled = settings == null || settings.isAutomationEnabled();
            boolean isManualOverride = settings != null && settings.getOverrideUntilTimestamp() > now;

            if (!automationEnabled) {
                Log.d(TAG, "ContextEngine: Automation disabled in settings. Skipping execution.");
                return;
            }

            // 1. Gather GPS geofence states
            List<ActiveGeofenceStateEntity> insideStates = database.activeGeofenceStateDao().getInsideStatesSync();
            List<BlockEntity> activeBlocks = new ArrayList<>();
            if (insideStates != null) {
                for (ActiveGeofenceStateEntity state : insideStates) {
                    BlockEntity block = database.blockDao().getBlockById(state.getBlockId());
                    if (block != null && block.isEnabled()) {
                        activeBlocks.add(block);
                    }
                }
            }

            // 2. Gather active Timetable schedule
            TimetableEntity activeSchedule = findActiveTimetableSlot(activeBlocks);

            // 3. Resolve dominant rule via PriorityResolver
            PriorityResolver.ResolutionResult decision = PriorityResolver.resolve(
                    isEmergencyActive,
                    isManualOverride,
                    activeSchedule,
                    activeBlocks
            );

            // 4. Apply sound profile safely
            String previousMode = soundModeManager.getCurrentRingerMode();
            if (!previousMode.equalsIgnoreCase(decision.soundMode)) {
                soundModeManager.savePreviousMode();
            }

            SoundModeManager.SoundChangeResult changeResult = soundModeManager.applySoundModeWithResult(decision.soundMode);
            String actualAppliedMode = changeResult.actualMode;

            // 5. Persist structured decision & history
            String formattedTime = new SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault()).format(new Date());

            AutomationDecisionEntity decisionEntity = new AutomationDecisionEntity(
                    now, formattedTime, decision.source, decision.location,
                    previousMode, actualAppliedMode, decision.reason, decision.confidence, decision.priority
            );
            database.automationDecisionDao().insert(decisionEntity);

            HistoryEntity historyEntity = new HistoryEntity(
                    now, formattedTime, decision.location,
                    decision.reason, previousMode, actualAppliedMode
            );
            database.historyDao().insert(historyEntity);

            // 6. Post structured ContextState object
            ContextState state = new ContextState(
                    activeBlocks, activeSchedule,
                    isEmergencyActive, isManualOverride,
                    actualAppliedMode, decision.confidence,
                    decision.source, decision.reason, decision.priority, now
            );
            mainHandler.post(() -> liveContextState.setValue(state));

            // 7. Wearable & Notification dispatch
            if (settings == null || settings.isNotificationsEnabled()) {
                if (!previousMode.equalsIgnoreCase(actualAppliedMode)) {
                    wearableHelper.notifyZoneTransition(
                            "Sound Profile: " + actualAppliedMode,
                            decision.reason,
                            "AUTO_RESTORE".equals(decision.source) ? "EXIT" : "ENTER"
                    );
                }
            }

            Log.i(TAG, "Context Engine Decision: [" + decision.source + " -> " + actualAppliedMode + "] Reason: " + decision.reason);
        });
    }

    private TimetableEntity findActiveTimetableSlot(List<BlockEntity> activeBlocks) {
        String currentDay = Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        if (activeBlocks != null && !activeBlocks.isEmpty()) {
            for (BlockEntity block : activeBlocks) {
                List<TimetableEntity> list = database.timetableDao().getEntriesForBlockSync(block.getId());
                if (list != null) {
                    for (TimetableEntity t : list) {
                        if (t.isEnabled() && t.getDayOfWeek().equalsIgnoreCase(currentDay)) {
                            if (isTimeBetween(currentTime, t.getStartTime(), t.getEndTime())) {
                                return t;
                            }
                        }
                    }
                }
            }
        }

        // Fallback: Check all enabled timetable slots for the current day
        List<TimetableEntity> allEnabled = database.timetableDao().getEnabledTimetablesSync();
        if (allEnabled != null) {
            for (TimetableEntity t : allEnabled) {
                if (t.isEnabled() && t.getDayOfWeek().equalsIgnoreCase(currentDay)) {
                    if (isTimeBetween(currentTime, t.getStartTime(), t.getEndTime())) {
                        return t;
                    }
                }
            }
        }

        return null;
    }

    private boolean isTimeBetween(String targetTime, String startTime, String endTime) {
        return TimeUtils.isTimeBetween(targetTime, startTime, endTime);
    }
}
