package com.blocksilent.app.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BLOCKSILENT_GEOFENCE";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "BLOCKSILENT_GEOFENCE: onReceive broadcast triggered in background");

        if (intent == null) {
            Log.e(TAG, "BLOCKSILENT_GEOFENCE: Null intent received");
            return;
        }

        final PendingResult pendingResult = goAsync();

        try {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            GeofenceEventProcessor processor = new GeofenceEventProcessor(context);
            processor.processGeofencingEvent(geofencingEvent);
        } catch (Exception e) {
            Log.e(TAG, "BLOCKSILENT_GEOFENCE: Exception processing geofence broadcast", e);
        } finally {
            try {
                pendingResult.finish();
            } catch (Exception ignored) {
            }
        }
    }
}
