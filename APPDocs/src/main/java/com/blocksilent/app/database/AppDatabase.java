package com.blocksilent.app.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.ContactEntity;
import com.blocksilent.app.database.entities.HistoryEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.database.entities.TimetableEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        BlockEntity.class,
        TimetableEntity.class,
        HistoryEntity.class,
        ContactEntity.class,
        SettingsEntity.class,
        com.blocksilent.app.database.entities.ActiveGeofenceStateEntity.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract BlockDao blockDao();
    public abstract TimetableDao timetableDao();
    public abstract HistoryDao historyDao();
    public abstract ContactDao contactDao();
    public abstract SettingsDao settingsDao();
    public abstract ActiveGeofenceStateDao activeGeofenceStateDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "blocksilent_database"
                    )
                    .addCallback(sRoomDatabaseCallback)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                // Populate initial sample blocks required by specification
                BlockDao blockDao = INSTANCE.blockDao();

                // Initial sample blocks:
                // 1. CSE Block - Silent - 50m
                blockDao.insert(new BlockEntity("CSE Block", 12.9716, 77.5946, 50.0f, "SILENT", true, 2));

                // 2. Library - Silent - 40m
                blockDao.insert(new BlockEntity("Library", 12.9720, 77.5950, 40.0f, "SILENT", true, 4));

                // 3. Computer Lab - Vibrate - 50m
                blockDao.insert(new BlockEntity("Computer Lab", 12.9710, 77.5940, 50.0f, "VIBRATE", true, 3));

                // 4. Canteen - Normal - 50m
                blockDao.insert(new BlockEntity("Canteen", 12.9725, 77.5955, 50.0f, "NORMAL", true, 5));

                // 5. Auditorium - Silent - 60m
                blockDao.insert(new BlockEntity("Auditorium", 12.9730, 77.5960, 60.0f, "SILENT", true, 1));

                // Populate initial Settings
                SettingsDao settingsDao = INSTANCE.settingsDao();
                settingsDao.insertOrUpdate(new SettingsEntity(true, true, "NORMAL", 0, true));

                // Populate initial sample Important Contact
                ContactDao contactDao = INSTANCE.contactDao();
                contactDao.insert(new ContactEntity("Parents / Guardian", "+1234567890", true));
            });
        }
    };
}
