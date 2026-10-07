package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "blocks")
public class BlockEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private double latitude;
    private double longitude;
    private float radius; // in meters
    private String soundMode; // "SILENT", "VIBRATE", "NORMAL"
    private boolean enabled;
    private int priority; // Priority ranking (e.g. Exam Hall=1, Classroom=2, Lab=3, Library=4, Other=5)

    public BlockEntity(String name, double latitude, double longitude, float radius, String soundMode, boolean enabled, int priority) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.soundMode = soundMode;
        this.enabled = enabled;
        this.priority = priority;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
