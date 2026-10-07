package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.blocksilent.app.database.entities.WearableSettingsEntity;

@Dao
public interface WearableSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(WearableSettingsEntity settings);

    @Query("SELECT * FROM wearable_settings WHERE id = 1 LIMIT 1")
    LiveData<WearableSettingsEntity> getWearableSettings();

    @Query("SELECT * FROM wearable_settings WHERE id = 1 LIMIT 1")
    WearableSettingsEntity getWearableSettingsSync();

    @Query("DELETE FROM wearable_settings")
    void deleteAll();
}
