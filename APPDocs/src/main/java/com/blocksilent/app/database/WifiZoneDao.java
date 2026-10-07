package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.blocksilent.app.database.entities.WifiZoneEntity;

import java.util.List;

@Dao
public interface WifiZoneDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdate(WifiZoneEntity wifiZone);

    @Update
    void update(WifiZoneEntity wifiZone);

    @Delete
    void delete(WifiZoneEntity wifiZone);

    @Query("SELECT * FROM wifi_zones ORDER BY id DESC")
    LiveData<List<WifiZoneEntity>> getAllWifiZones();

    @Query("SELECT * FROM wifi_zones ORDER BY id DESC")
    List<WifiZoneEntity> getAllWifiZonesSync();

    @Query("SELECT * FROM wifi_zones WHERE isEnabled = 1")
    List<WifiZoneEntity> getEnabledWifiZonesSync();

    @Query("SELECT * FROM wifi_zones WHERE UPPER(bssid) = UPPER(:bssid) AND isEnabled = 1 LIMIT 1")
    WifiZoneEntity findByBssidSync(String bssid);

    @Query("SELECT * FROM wifi_zones WHERE id = :id LIMIT 1")
    WifiZoneEntity getByIdSync(long id);

    @Query("SELECT * FROM wifi_zones WHERE blockId = :blockId")
    List<WifiZoneEntity> getZonesForBlockSync(long blockId);

    @Query("DELETE FROM wifi_zones")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM wifi_zones")
    int getCountSync();
}
