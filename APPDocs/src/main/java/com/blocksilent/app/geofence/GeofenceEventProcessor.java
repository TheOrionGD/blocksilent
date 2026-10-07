package com.blocksilent.app.geofence;

import android.content.Context;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.utils.RuleEngine;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeofenceEventProcessor {
    private static final String TAG = "BLOCKSILENT_GEOFENCE";
    private static final long DEBOUNCE_WINDOW_MS = 6000; // 6 seconds debounce per geofence

    private static final Map<String, Long> sLastEventTimestamps = new ConcurrentHashMap<>();
    private static final Map<String, Integer> sLastEventTransitions = new ConcurrentHashMap<>();

    private final Context context;
    private final RuleEngine ruleEngine;

    public GeofenceEventProcessor(Context context) {
        this.context = context.getApplicationContext();
        this.ruleEngine = new RuleEngine(this.context);
    }

    /**
     * Safely process Android Location Services GeofencingEvent in background.
     */
    public void processGeofencingEvent(GeofencingEvent geofencingEvent) {
        if (geofencingEvent == null) {
            Log.e(TAG, "BLOCKSILENT_GEOFENCE: Null GeofencingEvent received");
            return;
        }

        if (geofencingEvent.hasError()) {
            int errorCode = geofencingEvent.getErrorCode();
            Log.e(TAG, "BLOCKSILENT_GEOFENCE: GeofencingEvent error code: " + errorCode);
            return;
        }

        int transitionType = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        if (triggeringGeofences == null || triggeringGeofences.isEmpty()) {
            Log.w(TAG, "BLOCKSILENT_GEOFENCE: No triggering geofences in event");
            return;
        }

        String transitionName = getTransitionString(transitionType);
        Log.d(TAG, "BLOCKSILENT_GEOFENCE: Transition detected: " + transitionName + " for " + triggeringGeofences.size() + " geofence(s)");

        for (Geofence geofence : triggeringGeofences) {
            String requestId = geofence.getRequestId();
            if (requestId == null) continue;

            // Debounce check to prevent duplicate rapid triggers
            long now = System.currentTimeMillis();
            Long lastTime = sLastEventTimestamps.get(requestId);
            Integer lastTrans = sLastEventTransitions.get(requestId);

            if (lastTime != null && lastTrans != null && lastTrans == transitionType && (now - lastTime < DEBOUNCE_WINDOW_MS)) {
                Log.d(TAG, "BLOCKSILENT_GEOFENCE: Debounced duplicate event for " + requestId + " (" + transitionName + ")");
                continue;
            }

            sLastEventTimestamps.put(requestId, now);
            sLastEventTransitions.put(requestId, transitionType);

            long blockId = parseBlockIdFromRequestId(requestId);
            if (blockId <= 0) {
                Log.w(TAG, "BLOCKSILENT_GEOFENCE: Could not parse block ID from requestId: " + requestId);
                continue;
            }

            AppDatabase.databaseWriteExecutor.execute(() -> {
                Log.d(TAG, "BLOCKSILENT_BACKGROUND: Processing background transition " + transitionName + " for block " + blockId);
                ruleEngine.processBlockTransition(blockId, transitionName);
            });
        }
    }

    /**
     * Process simulated or demo event through the identical pipeline.
     */
    public void processDemoEvent(long blockId, String transitionType) {
        Log.d(TAG, "BLOCKSILENT_GEOFENCE: Demo event invoked: " + transitionType + " for blockId=" + blockId);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ruleEngine.processBlockTransition(blockId, transitionType);
        });
    }

    public static long parseBlockIdFromRequestId(String requestId) {
        try {
            if (requestId.startsWith("block_")) {
                return Long.parseLong(requestId.substring(6));
            }
            return Long.parseLong(requestId);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "ENTER";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "EXIT";
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "DWELL";
            default:
                return "UNKNOWN";
        }
    }
}
