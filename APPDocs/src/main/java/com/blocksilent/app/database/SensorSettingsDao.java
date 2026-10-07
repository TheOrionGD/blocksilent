package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.blocksilent.app.database.entities.SensorSettingsEntity;

@Dao
public interface SensorSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SensorSettingsEntity settings);

    @Query("SELECT * FROM sensor_settings WHERE id = 1 LIMIT 1")
    LiveData<SensorSettingsEntity> getSensorSettings();

    @Query("SELECT * FROM sensor_settings WHERE id = 1 LIMIT 1")
    SensorSettingsEntity getSensorSettingsSync();

    @Query("DELETE FROM sensor_settings")
    void deleteAll();
}
