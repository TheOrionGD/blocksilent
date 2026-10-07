package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "settings")
public class SettingsEntity {
    @PrimaryKey
    private int id = 1;

    private boolean automationEnabled;
    private boolean notificationsEnabled;
    private String defaultSoundMode; // "NORMAL", "SILENT", "VIBRATE"
    private long overrideUntilTimestamp; // 0 if no active emergency override
    private boolean firstTimeLaunch;

    public SettingsEntity(boolean automationEnabled, boolean notificationsEnabled, String defaultSoundMode, long overrideUntilTimestamp, boolean firstTimeLaunch) {
        this.id = 1;
        this.automationEnabled = automationEnabled;
        this.notificationsEnabled = notificationsEnabled;
        this.defaultSoundMode = defaultSoundMode;
        this.overrideUntilTimestamp = overrideUntilTimestamp;
        this.firstTimeLaunch = firstTimeLaunch;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isAutomationEnabled() {
        return automationEnabled;
    }

    public void setAutomationEnabled(boolean automationEnabled) {
        this.automationEnabled = automationEnabled;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getDefaultSoundMode() {
        return defaultSoundMode;
    }

    public void setDefaultSoundMode(String defaultSoundMode) {
        this.defaultSoundMode = defaultSoundMode;
    }

    public long getOverrideUntilTimestamp() {
        return overrideUntilTimestamp;
    }

    public void setOverrideUntilTimestamp(long overrideUntilTimestamp) {
        this.overrideUntilTimestamp = overrideUntilTimestamp;
    }

    public boolean isFirstTimeLaunch() {
        return firstTimeLaunch;
    }

    public void setFirstTimeLaunch(boolean firstTimeLaunch) {
        this.firstTimeLaunch = firstTimeLaunch;
    }
}
