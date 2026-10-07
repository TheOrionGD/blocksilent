package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.blocksilent.app.database.entities.ActiveGeofenceStateEntity;

import java.util.List;

@Dao
public interface ActiveGeofenceStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ActiveGeofenceStateEntity state);

    @Query("SELECT * FROM active_geofence_state WHERE blockId = :blockId LIMIT 1")
    ActiveGeofenceStateEntity getStateByBlockIdSync(long blockId);

    @Query("SELECT * FROM active_geofence_state WHERE blockId = :blockId LIMIT 1")
    LiveData<ActiveGeofenceStateEntity> getStateByBlockId(long blockId);

    @Query("SELECT * FROM active_geofence_state WHERE state = 'INSIDE'")
    List<ActiveGeofenceStateEntity> getInsideStatesSync();

    @Query("SELECT * FROM active_geofence_state WHERE state = 'INSIDE'")
    LiveData<List<ActiveGeofenceStateEntity>> getInsideStates();

    @Query("SELECT * FROM active_geofence_state")
    List<ActiveGeofenceStateEntity> getAllStatesSync();

    @Query("SELECT * FROM active_geofence_state")
    LiveData<List<ActiveGeofenceStateEntity>> getAllStates();

    @Query("DELETE FROM active_geofence_state WHERE blockId = :blockId")
    void deleteByBlockId(long blockId);

    @Query("DELETE FROM active_geofence_state")
    void deleteAll();
}
