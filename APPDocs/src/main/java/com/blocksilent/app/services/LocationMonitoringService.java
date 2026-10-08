package com.blocksilent.app.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.blocksilent.app.R;
import com.blocksilent.app.activities.MainActivity;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.utils.RuleEngine;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationMonitoringService extends Service {
    private static final String TAG = "LocationMonitoring";
    private static final String CHANNEL_ID = "blocksilent_service_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 2001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private RuleEngine ruleEngine;
    private AppDatabase database;
    private final Set<Long> insideBlockIds = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        ruleEngine = new RuleEngine(this);
        database = AppDatabase.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createServiceNotificationChannel();
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification("Active automatic campus block detection"));
        startLocationUpdates();
    }

    private void createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "BlockSilent Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitors campus location to automatically silence inside blocks and unmute outside.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildForegroundNotification(String statusText) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("BlockSilent Active")
                .setContentText(statusText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        // High accuracy location request with fast intervals to ensure immediate radius entry/exit detection
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .setMinUpdateDistanceMeters(3.0f)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        checkLocationAgainstBlocks(location);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing for service updates: " + e.getMessage());
        }
    }

    private void checkLocationAgainstBlocks(Location location) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<BlockEntity> enabledBlocks = database.blockDao().getEnabledBlocksSync();
            if (enabledBlocks == null || enabledBlocks.isEmpty()) {
                if (!insideBlockIds.isEmpty()) {
                    insideBlockIds.clear();
                    ruleEngine.processOutsideAllBlocks();
                }
                return;
            }

            boolean wasPreviouslyInside = !insideBlockIds.isEmpty();
            Set<Long> currentlyInside = new HashSet<>();

            float accuracy = (location != null && location.hasAccuracy()) ? location.getAccuracy() : 10.0f;
            float accuracyBuffer = Math.min(accuracy * 0.5f, 15.0f);

            for (BlockEntity block : enabledBlocks) {
                float[] results = new float[1];
                Location.distanceBetween(
                        location.getLatitude(), location.getLongitude(),
                        block.getLatitude(), block.getLongitude(),
                        results
                );
                float distance = results[0];

                float configuredRadius = block.getRadius();
                // Effective entry radius compensates for real-world GPS tolerance (minimum 25m)
                float entryRadius = Math.max(configuredRadius + accuracyBuffer, 25.0f);
                // Exit hysteresis adds a 15m buffer beyond entry radius to prevent premature exits due to GPS jitter
                float exitRadius = entryRadius + 15.0f;

                boolean isAlreadyInside = insideBlockIds.contains(block.getId());

                if (!isAlreadyInside) {
                    // Check for NEW ENTRY
                    if (distance <= entryRadius) {
                        currentlyInside.add(block.getId());
                        insideBlockIds.add(block.getId());
                        Log.d(TAG, "AUTOMATIC ENTRY: Inside " + block.getName() + " (Distance: " + String.format(java.util.Locale.US, "%.1f", distance) + "m, Entry Radius: " + entryRadius + "m)");
                        ruleEngine.processBlockEntry(block.getId());
                    }
                } else {
                    // Check if STILL INSIDE (Hysteresis check up to exitRadius)
                    if (distance <= exitRadius) {
                        currentlyInside.add(block.getId());
                    } else {
                        Log.d(TAG, "AUTOMATIC EXIT BOUNDARY BREACHED: Distance " + String.format(java.util.Locale.US, "%.1f", distance) + "m > Exit Radius " + exitRadius + "m for " + block.getName());
                    }
                }
            }

            // Detect any blocks that the user has moved completely outside the exit radius of
            Set<Long> exitedBlocks = new HashSet<>(insideBlockIds);
            exitedBlocks.removeAll(currentlyInside);

            for (Long exitedId : exitedBlocks) {
                Log.d(TAG, "AUTOMATIC EXIT CONFIRMED: Outside hysteresis zone of block ID " + exitedId);
                insideBlockIds.remove(exitedId);
                ruleEngine.processBlockExit(exitedId);
            }

            // If completely outside all blocks, ensure phone sound mode is restored
            if (currentlyInside.isEmpty() && (wasPreviouslyInside || !exitedBlocks.isEmpty())) {
                insideBlockIds.clear();
                ruleEngine.processOutsideAllBlocks();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createServiceNotificationChannel();
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification("Active automatic campus block detection"));
        startLocationUpdates();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
