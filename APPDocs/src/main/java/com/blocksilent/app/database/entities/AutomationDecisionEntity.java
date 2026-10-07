package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "automation_decisions")
public class AutomationDecisionEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestamp;
    private String formattedTime;
    private String source; // "GEO", "WIFI", "TIMETABLE", "SENSOR", "EMERGENCY", "MANUAL", "COMPOSITE"
    private String location;
    private String previousMode;
    private String newMode;
    private String reason;
    private String confidence; // "HIGH", "MEDIUM", "LOW"
    private int priority;

    public AutomationDecisionEntity(long timestamp, String formattedTime, String source,
                                    String location, String previousMode, String newMode,
                                    String reason, String confidence, int priority) {
        this.timestamp = timestamp;
        this.formattedTime = formattedTime != null ? formattedTime : "";
        this.source = source != null ? source : "UNKNOWN";
        this.location = location != null ? location : "Unknown Location";
        this.previousMode = previousMode != null ? previousMode : "NORMAL";
        this.newMode = newMode != null ? newMode : "NORMAL";
        this.reason = reason != null ? reason : "";
        this.confidence = confidence != null ? confidence : "MEDIUM";
        this.priority = priority;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPreviousMode() { return previousMode; }
    public void setPreviousMode(String previousMode) { this.previousMode = previousMode; }

    public String getNewMode() { return newMode; }
    public void setNewMode(String newMode) { this.newMode = newMode; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}
