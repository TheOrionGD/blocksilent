package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.blocksilent.app.database.entities.EmergencyContactEntity;

import java.util.List;

@Dao
public interface EmergencyContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrUpdate(EmergencyContactEntity contact);

    @Update
    void update(EmergencyContactEntity contact);

    @Delete
    void delete(EmergencyContactEntity contact);

    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC, name ASC")
    LiveData<List<EmergencyContactEntity>> getAllContacts();

    @Query("SELECT * FROM emergency_contacts ORDER BY priority ASC, name ASC")
    List<EmergencyContactEntity> getAllContactsSync();

    @Query("SELECT * FROM emergency_contacts WHERE phoneNumber = :phone LIMIT 1")
    EmergencyContactEntity findByPhoneSync(String phone);

    @Query("SELECT * FROM emergency_contacts WHERE isKeywordEnabled = 1")
    List<EmergencyContactEntity> getKeywordAuthorizedContactsSync();

    @Query("UPDATE emergency_contacts SET lastTriggerTimestamp = :timestamp WHERE id = :id")
    void updateLastTriggerTimestamp(long id, long timestamp);

    @Query("DELETE FROM emergency_contacts")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    int getCountSync();
}
