package com.blocksilent.app.geofence;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.utils.PermissionManager;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GeofenceManager {
    private static final String TAG = "BLOCKSILENT_GEOFENCE";
    private static final String PREFS_NAME = "geofence_manager_prefs";
    private static final String KEY_REGISTERED_COUNT = "registered_geofence_count";
    private static final String KEY_LAST_STATUS = "last_registration_status";
    private static final String KEY_LAST_ERROR = "last_registration_error";

    public static final int MAX_GEOFENCES = 100; // Android OS limitation

    private final Context context;
    private final GeofencingClient geofencingClient;
    private final SharedPreferences prefs;
    private PendingIntent geofencePendingIntent;

    private static final Set<String> sRegisteredGeofenceIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(this.context);
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        intent.setAction("com.blocksilent.app.ACTION_GEOFENCE_EVENT");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        geofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        return geofencePendingIntent;
    }

    public static String getRequestIdForBlock(long blockId) {
        return "block_" + blockId;
    }

    /**
     * Register a single block geofence.
     */
    @SuppressLint("MissingPermission")
    public void registerGeofence(BlockEntity block) {
        if (block == null || !block.isEnabled()) {
            if (block != null) {
                removeGeofence(getRequestIdForBlock(block.getId()));
            }
            return;
        }

        List<BlockEntity> singleList = new ArrayList<>();
        singleList.add(block);
        registerGeofences(singleList);
    }

    /**
     * Loads all enabled blocks from Room DB and registers them.
     */
    public void registerAllEnabledGeofences() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            List<BlockEntity> enabledBlocks = db.blockDao().getEnabledBlocksSync();
            if (enabledBlocks != null) {
                registerGeofences(enabledBlocks);
            } else {
                removeAllGeofences();
            }
        });
    }

    public void reRegisterAllGeofences() {
        Log.d(TAG, "BLOCKSILENT_GEOFENCE: Re-registering all enabled geofences");
        registerAllEnabledGeofences();
    }

    /**
     * Registers a list of blocks with Android GeofencingClient.
     */
    @SuppressLint("MissingPermission")
    public void registerGeofences(List<BlockEntity> blocks) {
        if (!PermissionManager.hasLocationPermission(context)) {
            handleRegistrationFailure("Foreground location permission not granted");
            return;
        }

        if (blocks == null || blocks.isEmpty()) {
            removeAllGeofences();
            return;
        }

        List<Geofence> geofenceList = new ArrayList<>();
        for (BlockEntity block : blocks) {
            if (block.isEnabled()) {
                if (geofenceList.size() >= MAX_GEOFENCES) {
                    Log.w(TAG, "BLOCKSILENT_GEOFENCE: Reached MAX_GEOFENCES (" + MAX_GEOFENCES + "). Skipping remaining.");
                    break;
                }

                String reqId = getRequestIdForBlock(block.getId());
                float radius = Math.max(block.getRadius(), 25.0f); // Sensible min radius

                Geofence geofence = new Geofence.Builder()
                        .setRequestId(reqId)
                        .setCircularRegion(block.getLatitude(), block.getLongitude(), radius)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL)
                        .setLoiteringDelay(3000) // 3s dwell
                        .setNotificationResponsiveness(2000) // 2s responsiveness
                        .build();

                geofenceList.add(geofence);
                sRegisteredGeofenceIds.add(reqId);
                Log.d(TAG, "BLOCKSILENT_GEOFENCE: Prepared " + block.getName() + " [reqId=" + reqId + ", radius=" + radius + "m, mode=" + block.getSoundMode() + "]");
            }
        }

        if (geofenceList.isEmpty()) {
            removeAllGeofences();
            return;
        }

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofences(geofenceList)
                .build();

        try {
            geofencingClient.addGeofences(request, getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> {
                        int count = geofenceList.size();
                        prefs.edit()
                                .putInt(KEY_REGISTERED_COUNT, count)
                                .putString(KEY_LAST_STATUS, "ACTIVE")
                                .remove(KEY_LAST_ERROR)
                                .apply();
                        Log.d(TAG, "BLOCKSILENT_GEOFENCE: Successfully registered " + count + " active geofence(s) with Android Location Services");
                    })
                    .addOnFailureListener(e -> {
                        handleRegistrationFailure(e.getMessage());
                    });
        } catch (SecurityException se) {
            handleRegistrationFailure("SecurityException: " + se.getMessage());
        } catch (Exception e) {
            handleRegistrationFailure("Exception: " + e.getMessage());
        }
    }

    /**
     * Remove a single geofence by ID.
     */
    public void removeGeofence(String geofenceId) {
        if (geofenceId == null) return;
        List<String> list = new ArrayList<>();
        list.add(geofenceId);
        geofencingClient.removeGeofences(list)
                .addOnSuccessListener(aVoid -> {
                    sRegisteredGeofenceIds.remove(geofenceId);
                    Log.d(TAG, "BLOCKSILENT_GEOFENCE: Removed geofence " + geofenceId);
                })
                .addOnFailureListener(e -> Log.w(TAG, "BLOCKSILENT_GEOFENCE: Failed to remove geofence " + geofenceId + ": " + e.getMessage()));
    }

    /**
     * Remove all registered geofences.
     */
    public void removeAllGeofences() {
        try {
            geofencingClient.removeGeofences(getGeofencePendingIntent())
                    .addOnSuccessListener(aVoid -> {
                        sRegisteredGeofenceIds.clear();
                        prefs.edit()
                                .putInt(KEY_REGISTERED_COUNT, 0)
                                .putString(KEY_LAST_STATUS, "IDLE")
                                .remove(KEY_LAST_ERROR)
                                .apply();
                        Log.d(TAG, "BLOCKSILENT_GEOFENCE: Removed all registered geofences.");
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "BLOCKSILENT_GEOFENCE: Failed to remove all geofences: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "BLOCKSILENT_GEOFENCE: Error removing all geofences", e);
        }
    }

    public void unregisterAllGeofences() {
        removeAllGeofences();
    }

    public boolean isGeofenceRegistered(long blockId) {
        return sRegisteredGeofenceIds.contains(getRequestIdForBlock(blockId));
    }

    public int getRegisteredGeofencesCount() {
        return prefs.getInt(KEY_REGISTERED_COUNT, sRegisteredGeofenceIds.size());
    }

    public String getRegistrationStatus() {
        return prefs.getString(KEY_LAST_STATUS, "IDLE");
    }

    public String getRegistrationError() {
        return prefs.getString(KEY_LAST_ERROR, null);
    }

    public void handleRegistrationFailure(String reason) {
        Log.e(TAG, "BLOCKSILENT_GEOFENCE: Geofence registration failed: " + reason);
        prefs.edit()
                .putString(KEY_LAST_STATUS, "FAILED")
                .putString(KEY_LAST_ERROR, reason)
                .apply();
    }
}
