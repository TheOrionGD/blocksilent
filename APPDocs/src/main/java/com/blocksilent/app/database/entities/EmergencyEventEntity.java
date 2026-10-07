package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "emergency_events")
public class EmergencyEventEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestamp;
    private String formattedTime;
    private String contactName;
    private String contactPhone;
    private String eventType; // "MISSED_CALL_SMS", "KEYWORD_TRIGGER", "MANUAL_OVERRIDE", "SIREN_ACTIVATED"
    private String details;
    private boolean sirenTriggered;

    public EmergencyEventEntity(long timestamp, String formattedTime, String contactName,
                                String contactPhone, String eventType, String details, boolean sirenTriggered) {
        this.timestamp = timestamp;
        this.formattedTime = formattedTime != null ? formattedTime : "";
        this.contactName = contactName != null ? contactName : "";
        this.contactPhone = contactPhone != null ? contactPhone : "";
        this.eventType = eventType != null ? eventType : "";
        this.details = details != null ? details : "";
        this.sirenTriggered = sirenTriggered;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public boolean isSirenTriggered() { return sirenTriggered; }
    public void setSirenTriggered(boolean sirenTriggered) { this.sirenTriggered = sirenTriggered; }
}
