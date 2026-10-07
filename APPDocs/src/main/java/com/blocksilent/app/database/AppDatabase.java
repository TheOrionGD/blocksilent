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
    version = 3,
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
                // Initialize default application settings with clean database
                SettingsDao settingsDao = INSTANCE.settingsDao();
                settingsDao.insertOrUpdate(new SettingsEntity(true, true, "NORMAL", 0, true));
            });
        }
    };
}
