package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "wifi_zones",
    indices = {@Index(value = {"bssid"}, unique = true), @Index(value = {"blockId"})}
)
public class WifiZoneEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String bssid;
    private String ssid;
    private int rssiThreshold; // e.g. -75 dBm minimum
    private String roomName;
    private String floor;
    private long blockId; // Associated BlockEntity id
    private String blockName;
    private String soundPolicy; // "SILENT", "VIBRATE", "NORMAL", "DEFAULT"
    private boolean isEnabled;
    private long createdAt;

    public WifiZoneEntity(String bssid, String ssid, int rssiThreshold, String roomName,
                          String floor, long blockId, String blockName, String soundPolicy, boolean isEnabled, long createdAt) {
        this.bssid = bssid != null ? bssid.toUpperCase().trim() : "";
        this.ssid = ssid != null ? ssid : "";
        this.rssiThreshold = rssiThreshold;
        this.roomName = roomName != null ? roomName : "";
        this.floor = floor != null ? floor : "1";
        this.blockId = blockId;
        this.blockName = blockName != null ? blockName : "";
        this.soundPolicy = soundPolicy != null ? soundPolicy : "SILENT";
        this.isEnabled = isEnabled;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getBssid() { return bssid; }
    public void setBssid(String bssid) { this.bssid = bssid != null ? bssid.toUpperCase().trim() : ""; }

    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }

    public int getRssiThreshold() { return rssiThreshold; }
    public void setRssiThreshold(int rssiThreshold) { this.rssiThreshold = rssiThreshold; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public long getBlockId() { return blockId; }
    public void setBlockId(long blockId) { this.blockId = blockId; }

    public String getBlockName() { return blockName; }
    public void setBlockName(String blockName) { this.blockName = blockName; }

    public String getSoundPolicy() { return soundPolicy; }
    public void setSoundPolicy(String soundPolicy) { this.soundPolicy = soundPolicy; }

    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
