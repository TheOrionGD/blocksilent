package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "active_geofence_state")
public class ActiveGeofenceStateEntity {
    @PrimaryKey
    private long blockId;

    private String blockName;
    private String state; // "INSIDE" or "OUTSIDE"
    private String previousSoundMode; // Mode before entering this block
    private String appliedSoundMode;  // Mode applied when inside
    private long enteredAt;
    private String lastTransition; // "ENTER", "EXIT", "DWELL"
    private long lastTransitionTimestamp;

    public ActiveGeofenceStateEntity(long blockId, String blockName, String state, String previousSoundMode, String appliedSoundMode, long enteredAt, String lastTransition, long lastTransitionTimestamp) {
        this.blockId = blockId;
        this.blockName = blockName;
        this.state = state;
        this.previousSoundMode = previousSoundMode;
        this.appliedSoundMode = appliedSoundMode;
        this.enteredAt = enteredAt;
        this.lastTransition = lastTransition;
        this.lastTransitionTimestamp = lastTransitionTimestamp;
    }

    public long getBlockId() {
        return blockId;
    }

    public void setBlockId(long blockId) {
        this.blockId = blockId;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPreviousSoundMode() {
        return previousSoundMode;
    }

    public void setPreviousSoundMode(String previousSoundMode) {
        this.previousSoundMode = previousSoundMode;
    }

    public String getAppliedSoundMode() {
        return appliedSoundMode;
    }

    public void setAppliedSoundMode(String appliedSoundMode) {
        this.appliedSoundMode = appliedSoundMode;
    }

    public long getEnteredAt() {
        return enteredAt;
    }

    public void setEnteredAt(long enteredAt) {
        this.enteredAt = enteredAt;
    }

    public String getLastTransition() {
        return lastTransition;
    }

    public void setLastTransition(String lastTransition) {
        this.lastTransition = lastTransition;
    }

    public long getLastTransitionTimestamp() {
        return lastTransitionTimestamp;
    }

    public void setLastTransitionTimestamp(long lastTransitionTimestamp) {
        this.lastTransitionTimestamp = lastTransitionTimestamp;
    }
}
