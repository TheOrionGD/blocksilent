package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "emergency_contacts",
    indices = {@Index(value = {"phoneNumber"}, unique = true)}
)
public class EmergencyContactEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String phoneNumber;
    private int priority; // 1 = Highest (Family/Dean), 2 = High (Professors), 3 = Normal
    private boolean isSmsEnabled; // Send auto status SMS on missed call
    private boolean isKeywordEnabled; // Allow BREAK-GLASS keyword trigger
    private long lastTriggerTimestamp; // Anti-loop cooldown tracking
    private long createdAt;

    public EmergencyContactEntity(String name, String phoneNumber, int priority,
                                  boolean isSmsEnabled, boolean isKeywordEnabled,
                                  long lastTriggerTimestamp, long createdAt) {
        this.name = name != null ? name : "";
        this.phoneNumber = phoneNumber != null ? phoneNumber.replaceAll("[^0-9+]", "") : "";
        this.priority = priority;
        this.isSmsEnabled = isSmsEnabled;
        this.isKeywordEnabled = isKeywordEnabled;
        this.lastTriggerTimestamp = lastTriggerTimestamp;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber != null ? phoneNumber.replaceAll("[^0-9+]", "") : "";
    }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isSmsEnabled() { return isSmsEnabled; }
    public void setSmsEnabled(boolean smsEnabled) { isSmsEnabled = smsEnabled; }

    public boolean isKeywordEnabled() { return isKeywordEnabled; }
    public void setKeywordEnabled(boolean keywordEnabled) { isKeywordEnabled = keywordEnabled; }

    public long getLastTriggerTimestamp() { return lastTriggerTimestamp; }
    public void setLastTriggerTimestamp(long lastTriggerTimestamp) { this.lastTriggerTimestamp = lastTriggerTimestamp; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
