package com.blocksilent.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.blocksilent.app.database.entities.BlockEntity;

import java.util.List;

@Dao
public interface BlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(BlockEntity block);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BlockEntity> blocks);

    @Update
    void update(BlockEntity block);

    @Delete
    void delete(BlockEntity block);

    @Query("SELECT * FROM blocks ORDER BY priority ASC, name ASC")
    LiveData<List<BlockEntity>> getAllBlocks();

    @Query("SELECT * FROM blocks ORDER BY priority ASC, name ASC")
    List<BlockEntity> getAllBlocksSync();

    @Query("SELECT * FROM blocks WHERE enabled = 1 ORDER BY priority ASC")
    List<BlockEntity> getEnabledBlocksSync();

    @Query("SELECT * FROM blocks WHERE id = :id LIMIT 1")
    BlockEntity getBlockById(long id);

    @Query("SELECT COUNT(*) FROM blocks WHERE enabled = 1")
    int getEnabledBlocksCountSync();

    @Query("DELETE FROM blocks")
    void deleteAll();
}
