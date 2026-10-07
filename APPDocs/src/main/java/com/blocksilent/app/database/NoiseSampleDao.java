package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.blocksilent.app.database.entities.NoiseSampleEntity;

import java.util.List;

@Dao
public interface NoiseSampleDao {

    @Insert
    long insert(NoiseSampleEntity sample);

    @Query("SELECT * FROM noise_samples ORDER BY timestamp DESC")
    LiveData<List<NoiseSampleEntity>> getAllSamples();

    @Query("SELECT * FROM noise_samples ORDER BY timestamp DESC LIMIT 100")
    List<NoiseSampleEntity> getRecentSamplesSync();

    @Query("SELECT * FROM noise_samples WHERE blockId = :blockId ORDER BY timestamp DESC LIMIT 20")
    List<NoiseSampleEntity> getSamplesForBlockSync(long blockId);

    @Query("DELETE FROM noise_samples")
    void deleteAll();
}
