package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sensor_settings")
public class SensorSettingsEntity {

    @PrimaryKey
    private int id = 1; // Single row settings configuration
    private boolean flipToSilenceEnabled;
    private boolean pickupRestoreEnabled;
    private boolean requireLowLight;
    private boolean requireProximity;
    private long confirmationDurationMs; // 500ms, 1000ms, 1500ms, 2000ms
    private float customZThreshold; // -7.5 m/s^2 default

    public SensorSettingsEntity() {
        this.flipToSilenceEnabled = true;
        this.pickupRestoreEnabled = true;
        this.requireLowLight = false;
        this.requireProximity = true;
        this.confirmationDurationMs = 1000L;
        this.customZThreshold = -7.5f;
    }

    @androidx.room.Ignore
    public SensorSettingsEntity(boolean flipToSilenceEnabled, boolean pickupRestoreEnabled,
                                boolean requireLowLight, boolean requireProximity,
                                long confirmationDurationMs, float customZThreshold) {
        this.id = 1;
        this.flipToSilenceEnabled = flipToSilenceEnabled;
        this.pickupRestoreEnabled = pickupRestoreEnabled;
        this.requireLowLight = requireLowLight;
        this.requireProximity = requireProximity;
        this.confirmationDurationMs = confirmationDurationMs;
        this.customZThreshold = customZThreshold;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public boolean isFlipToSilenceEnabled() { return flipToSilenceEnabled; }
    public void setFlipToSilenceEnabled(boolean flipToSilenceEnabled) { this.flipToSilenceEnabled = flipToSilenceEnabled; }

    public boolean isPickupRestoreEnabled() { return pickupRestoreEnabled; }
    public void setPickupRestoreEnabled(boolean pickupRestoreEnabled) { this.pickupRestoreEnabled = pickupRestoreEnabled; }

    public boolean isRequireLowLight() { return requireLowLight; }
    public void setRequireLowLight(boolean requireLowLight) { this.requireLowLight = requireLowLight; }

    public boolean isRequireProximity() { return requireProximity; }
    public void setRequireProximity(boolean requireProximity) { this.requireProximity = requireProximity; }

    public long getConfirmationDurationMs() { return confirmationDurationMs; }
    public void setConfirmationDurationMs(long confirmationDurationMs) { this.confirmationDurationMs = confirmationDurationMs; }

    public float getCustomZThreshold() { return customZThreshold; }
    public void setCustomZThreshold(float customZThreshold) { this.customZThreshold = customZThreshold; }
}
