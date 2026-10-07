package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.blocksilent.app.database.entities.AutomationDecisionEntity;

import java.util.List;

@Dao
public interface AutomationDecisionDao {

    @Insert
    long insert(AutomationDecisionEntity decision);

    @Query("SELECT * FROM automation_decisions ORDER BY timestamp DESC")
    LiveData<List<AutomationDecisionEntity>> getAllDecisions();

    @Query("SELECT * FROM automation_decisions ORDER BY timestamp DESC LIMIT 50")
    List<AutomationDecisionEntity> getRecentDecisionsSync();

    @Query("SELECT * FROM automation_decisions ORDER BY timestamp DESC LIMIT 1")
    LiveData<AutomationDecisionEntity> getLatestDecision();

    @Query("SELECT * FROM automation_decisions ORDER BY timestamp DESC LIMIT 1")
    AutomationDecisionEntity getLatestDecisionSync();

    @Query("DELETE FROM automation_decisions")
    void deleteAll();
}
