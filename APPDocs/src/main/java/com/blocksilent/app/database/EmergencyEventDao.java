package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.blocksilent.app.database.entities.EmergencyEventEntity;

import java.util.List;

@Dao
public interface EmergencyEventDao {

    @Insert
    long insert(EmergencyEventEntity event);

    @Query("SELECT * FROM emergency_events ORDER BY timestamp DESC")
    LiveData<List<EmergencyEventEntity>> getAllEvents();

    @Query("SELECT * FROM emergency_events ORDER BY timestamp DESC LIMIT 50")
    List<EmergencyEventEntity> getRecentEventsSync();

    @Query("DELETE FROM emergency_events")
    void deleteAll();
}
