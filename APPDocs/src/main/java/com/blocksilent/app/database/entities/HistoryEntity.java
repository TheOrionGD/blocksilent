package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history")
public class HistoryEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long timestamp;
    private String formattedDateTime; // e.g. "10 Sep 2026 09:02 AM"
    private String blockName;
    private String eventType; // "Entered CSE Block", "Exited CSE Block", "Emergency Override Enabled", etc.
    private String previousMode; // "Normal", "Silent", "Vibrate"
    private String newMode;      // "Silent", "Vibrate", "Normal"

    public HistoryEntity(long timestamp, String formattedDateTime, String blockName, String eventType, String previousMode, String newMode) {
        this.timestamp = timestamp;
        this.formattedDateTime = formattedDateTime;
        this.blockName = blockName;
        this.eventType = eventType;
        this.previousMode = previousMode;
        this.newMode = newMode;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedDateTime() {
        return formattedDateTime;
    }

    public void setFormattedDateTime(String formattedDateTime) {
        this.formattedDateTime = formattedDateTime;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPreviousMode() {
        return previousMode;
    }

    public void setPreviousMode(String previousMode) {
        this.previousMode = previousMode;
    }

    public String getNewMode() {
        return newMode;
    }

    public void setNewMode(String newMode) {
        this.newMode = newMode;
    }
}
