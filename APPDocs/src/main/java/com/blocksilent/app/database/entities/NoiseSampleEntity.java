package com.blocksilent.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "noise_samples")
public class NoiseSampleEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long blockId;
    private String zoneName;
    private double approximateDb;
    private String noiseCategory; // "QUIET", "MODERATE", "HIGH"
    private long timestamp;
    private String formattedTime;

    public NoiseSampleEntity(long blockId, String zoneName, double approximateDb,
                             String noiseCategory, long timestamp, String formattedTime) {
        this.blockId = blockId;
        this.zoneName = zoneName != null ? zoneName : "Unknown Zone";
        this.approximateDb = approximateDb;
        this.noiseCategory = noiseCategory != null ? noiseCategory : "QUIET";
        this.timestamp = timestamp;
        this.formattedTime = formattedTime != null ? formattedTime : "";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getBlockId() { return blockId; }
    public void setBlockId(long blockId) { this.blockId = blockId; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }

    public double getApproximateDb() { return approximateDb; }
    public void setApproximateDb(double approximateDb) { this.approximateDb = approximateDb; }

    public String getNoiseCategory() { return noiseCategory; }
    public void setNoiseCategory(String noiseCategory) { this.noiseCategory = noiseCategory; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }
}
