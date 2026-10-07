package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.blocksilent.app.database.entities.HistoryEntity;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntity history);

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    LiveData<List<HistoryEntity>> getAllHistory();

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    List<HistoryEntity> getRecentHistorySync(int limit);

    @Query("SELECT COUNT(*) FROM history WHERE timestamp >= :startOfDayTimestamp")
    int getTodayChangesCount(long startOfDayTimestamp);

    @Query("SELECT COUNT(*) FROM history WHERE timestamp >= :startOfDayTimestamp")
    int getEventsCountSinceSync(long startOfDayTimestamp);

    @Query("DELETE FROM history")
    void deleteAll();
}
