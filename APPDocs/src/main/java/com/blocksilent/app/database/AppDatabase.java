package com.blocksilent.app.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.blocksilent.app.database.entities.ActiveGeofenceStateEntity;
import com.blocksilent.app.database.entities.AutomationDecisionEntity;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.ContactEntity;
import com.blocksilent.app.database.entities.EmergencyContactEntity;
import com.blocksilent.app.database.entities.EmergencyEventEntity;
import com.blocksilent.app.database.entities.HistoryEntity;
import com.blocksilent.app.database.entities.NoiseSampleEntity;
import com.blocksilent.app.database.entities.SensorSettingsEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.database.entities.TimetableEntity;
import com.blocksilent.app.database.entities.WearableSettingsEntity;
import com.blocksilent.app.database.entities.WifiZoneEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        BlockEntity.class,
        TimetableEntity.class,
        HistoryEntity.class,
        ContactEntity.class,
        SettingsEntity.class,
        ActiveGeofenceStateEntity.class,
        WifiZoneEntity.class,
        SensorSettingsEntity.class,
        EmergencyContactEntity.class,
        EmergencyEventEntity.class,
        NoiseSampleEntity.class,
        WearableSettingsEntity.class,
        AutomationDecisionEntity.class
    },
    version = 4,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract BlockDao blockDao();
    public abstract TimetableDao timetableDao();
    public abstract HistoryDao historyDao();
    public abstract ContactDao contactDao();
    public abstract SettingsDao settingsDao();
    public abstract ActiveGeofenceStateDao activeGeofenceStateDao();
    public abstract WifiZoneDao wifiZoneDao();
    public abstract SensorSettingsDao sensorSettingsDao();
    public abstract EmergencyContactDao emergencyContactDao();
    public abstract EmergencyEventDao emergencyEventDao();
    public abstract NoiseSampleDao noiseSampleDao();
    public abstract WearableSettingsDao wearableSettingsDao();
    public abstract AutomationDecisionDao automationDecisionDao();

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
                // Initialize default application settings
                SettingsDao settingsDao = INSTANCE.settingsDao();
                settingsDao.insertOrUpdate(new SettingsEntity(true, true, "NORMAL", 0, true));

                // Initialize default sensor and wearable settings
                SensorSettingsDao sensorSettingsDao = INSTANCE.sensorSettingsDao();
                sensorSettingsDao.insertOrUpdate(new SensorSettingsEntity(true, true, false, true, 1000L, -7.5f));

                WearableSettingsDao wearableSettingsDao = INSTANCE.wearableSettingsDao();
                wearableSettingsDao.insertOrUpdate(new WearableSettingsEntity(true, "SHORT_SHORT", "LONG_SHORT", "LONG_LONG_LONG"));
            });
        }
    };
}
