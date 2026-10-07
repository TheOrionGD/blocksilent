package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.blocksilent.app.database.entities.TimetableEntity;

import java.util.List;

@Dao
public interface TimetableDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TimetableEntity timetable);

    @Update
    void update(TimetableEntity timetable);

    @Delete
    void delete(TimetableEntity timetable);

    @Query("SELECT * FROM timetable ORDER BY dayOfWeek ASC, startTime ASC")
    LiveData<List<TimetableEntity>> getAllTimetables();

    @Query("SELECT * FROM timetable WHERE enabled = 1 ORDER BY startTime ASC")
    List<TimetableEntity> getEnabledTimetablesSync();

    @Query("SELECT * FROM timetable WHERE blockId = :blockId AND enabled = 1")
    List<TimetableEntity> getEntriesForBlockSync(long blockId);

    @Query("DELETE FROM timetable")
    void deleteAll();
}
