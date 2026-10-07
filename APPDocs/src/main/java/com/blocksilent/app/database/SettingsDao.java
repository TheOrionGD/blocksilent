package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.blocksilent.app.database.entities.SettingsEntity;

@Dao
public interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(SettingsEntity settings);

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    LiveData<SettingsEntity> getSettings();

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    SettingsEntity getSettingsSync();

    @Query("DELETE FROM settings")
    void deleteAll();
}
