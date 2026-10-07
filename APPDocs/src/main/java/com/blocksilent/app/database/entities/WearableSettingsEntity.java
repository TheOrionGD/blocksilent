package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wearable_settings")
public class WearableSettingsEntity {

    @PrimaryKey
    private int id = 1; // Single row configuration
    private boolean wearableSyncEnabled;
    private String enterHapticPattern; // "SHORT_SHORT", "LONG_SHORT", "SUBTLE", "NONE"
    private String exitHapticPattern;  // "LONG_SHORT", "SHORT", "SUBTLE", "NONE"
    private String emergencyHapticPattern; // "LONG_LONG_LONG", "SOS", "INTENSE"

    public WearableSettingsEntity() {
        this.wearableSyncEnabled = true;
        this.enterHapticPattern = "SHORT_SHORT";
        this.exitHapticPattern = "LONG_SHORT";
        this.emergencyHapticPattern = "LONG_LONG_LONG";
    }

    @androidx.room.Ignore
    public WearableSettingsEntity(boolean wearableSyncEnabled, String enterHapticPattern,
                                  String exitHapticPattern, String emergencyHapticPattern) {
        this.id = 1;
        this.wearableSyncEnabled = wearableSyncEnabled;
        this.enterHapticPattern = enterHapticPattern != null ? enterHapticPattern : "SHORT_SHORT";
        this.exitHapticPattern = exitHapticPattern != null ? exitHapticPattern : "LONG_SHORT";
        this.emergencyHapticPattern = emergencyHapticPattern != null ? emergencyHapticPattern : "LONG_LONG_LONG";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public boolean isWearableSyncEnabled() { return wearableSyncEnabled; }
    public void setWearableSyncEnabled(boolean wearableSyncEnabled) { this.wearableSyncEnabled = wearableSyncEnabled; }

    public String getEnterHapticPattern() { return enterHapticPattern; }
    public void setEnterHapticPattern(String enterHapticPattern) { this.enterHapticPattern = enterHapticPattern; }

    public String getExitHapticPattern() { return exitHapticPattern; }
    public void setExitHapticPattern(String exitHapticPattern) { this.exitHapticPattern = exitHapticPattern; }

    public String getEmergencyHapticPattern() { return emergencyHapticPattern; }
    public void setEmergencyHapticPattern(String emergencyHapticPattern) { this.emergencyHapticPattern = emergencyHapticPattern; }
}
