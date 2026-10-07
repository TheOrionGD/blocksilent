package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "timetable")
public class TimetableEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String subjectName;
    private String dayOfWeek; // "Monday", "Tuesday", etc.
    private String startTime; // e.g. "09:00 AM"
    private String endTime;   // e.g. "10:00 AM"
    private long blockId;
    private String blockName;
    private String soundMode; // "SILENT", "VIBRATE", "NORMAL"
    private boolean enabled;

    public TimetableEntity(String subjectName, String dayOfWeek, String startTime, String endTime, long blockId, String blockName, String soundMode, boolean enabled) {
        this.subjectName = subjectName;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.blockId = blockId;
        this.blockName = blockName;
        this.soundMode = soundMode;
        this.enabled = enabled;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
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

    public String getSoundMode() {
        return soundMode;
    }

    public void setSoundMode(String soundMode) {
        this.soundMode = soundMode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
