package com.blocksilent.app.context;

import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.TimetableEntity;
import com.blocksilent.app.database.entities.WifiZoneEntity;

import java.util.Collections;
import java.util.List;

public class ContextState {

    private final List<BlockEntity> activeGeofences;
    private final WifiZoneEntity activeWifiZone;
    private final TimetableEntity currentSchedule;
    private final boolean isFaceDownSilenced;
    private final boolean isEmergencyOverride;
    private final boolean isManualOverride;
    private final double approximateNoiseDb;
    private final String recommendedSoundMode; // "SILENT", "VIBRATE", "NORMAL"
    private final String confidence; // "HIGH", "MEDIUM", "LOW"
    private final String source; // "GEO", "WIFI", "TIMETABLE", "SENSOR", "EMERGENCY", "MANUAL", "COMPOSITE"
    private final String reason;
    private final int priority;
    private final long timestamp;

    public ContextState(List<BlockEntity> activeGeofences, WifiZoneEntity activeWifiZone,
                        TimetableEntity currentSchedule, boolean isFaceDownSilenced,
                        boolean isEmergencyOverride, boolean isManualOverride,
                        double approximateNoiseDb, String recommendedSoundMode,
                        String confidence, String source, String reason, int priority, long timestamp) {
        this.activeGeofences = activeGeofences != null ? activeGeofences : Collections.emptyList();
        this.activeWifiZone = activeWifiZone;
        this.currentSchedule = currentSchedule;
        this.isFaceDownSilenced = isFaceDownSilenced;
        this.isEmergencyOverride = isEmergencyOverride;
        this.isManualOverride = isManualOverride;
        this.approximateNoiseDb = approximateNoiseDb;
        this.recommendedSoundMode = recommendedSoundMode != null ? recommendedSoundMode : "NORMAL";
        this.confidence = confidence != null ? confidence : "HIGH";
        this.source = source != null ? source : "DEFAULT";
        this.reason = reason != null ? reason : "";
        this.priority = priority;
        this.timestamp = timestamp;
    }

    public List<BlockEntity> getActiveGeofences() { return activeGeofences; }
    public WifiZoneEntity getActiveWifiZone() { return activeWifiZone; }
    public TimetableEntity getCurrentSchedule() { return currentSchedule; }
    public boolean isFaceDownSilenced() { return isFaceDownSilenced; }
    public boolean isEmergencyOverride() { return isEmergencyOverride; }
    public boolean isManualOverride() { return isManualOverride; }
    public double getApproximateNoiseDb() { return approximateNoiseDb; }
    public String getRecommendedSoundMode() { return recommendedSoundMode; }
    public String getConfidence() { return confidence; }
    public String getSource() { return source; }
    public String getReason() { return reason; }
    public int getPriority() { return priority; }
    public long getTimestamp() { return timestamp; }

    public String getPrimaryLocationName() {
        if (activeWifiZone != null && activeWifiZone.getRoomName() != null && !activeWifiZone.getRoomName().isEmpty()) {
            return activeWifiZone.getRoomName() + " (Floor " + activeWifiZone.getFloor() + ")";
        }
        if (!activeGeofences.isEmpty()) {
            return activeGeofences.get(0).getName();
        }
        return "Outside Campus Blocks";
    }
}
