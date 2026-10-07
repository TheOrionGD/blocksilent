package com.blocksilent.app.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

public class LocationHelper {
    private static final String TAG = "LocationHelper";

    public interface LocationResultCallback {
        void onLocationFetched(Location location);
        void onError(String message);
    }

    public static boolean isGpsEnabled(Context context) {
        if (context == null) return false;
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return false;
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public static void fetchAccurateCurrentLocation(@NonNull Context context, @NonNull LocationResultCallback callback) {
        if (!PermissionManager.hasLocationPermission(context)) {
            callback.onError("Location permission not granted.");
            return;
        }

        if (!isGpsEnabled(context)) {
            callback.onError("Location services / GPS is disabled on device.");
            return;
        }

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        try {
            // Priority 1: High Accuracy Current Location
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null && isValidLocation(location)) {
                            Log.d(TAG, "Current high-accuracy location acquired: " + location.getLatitude() + ", " + location.getLongitude());
                            callback.onLocationFetched(location);
                        } else {
                            // Priority 2: Fused Last Location
                            tryFetchFusedLastLocation(context, fusedClient, callback);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "getCurrentLocation failed, trying last location", e);
                        tryFetchFusedLastLocation(context, fusedClient, callback);
                    });
        } catch (SecurityException se) {
            callback.onError("Permission error: " + se.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error requesting current location", e);
            tryFetchFusedLastLocation(context, fusedClient, callback);
        }
    }

    @SuppressLint("MissingPermission")
    private static void tryFetchFusedLastLocation(Context context, FusedLocationProviderClient fusedClient, LocationResultCallback callback) {
        try {
            fusedClient.getLastLocation()
                    .addOnSuccessListener(lastLoc -> {
                        if (lastLoc != null && isValidLocation(lastLoc)) {
                            Log.d(TAG, "Last known fused location acquired: " + lastLoc.getLatitude() + ", " + lastLoc.getLongitude());
                            callback.onLocationFetched(lastLoc);
                        } else {
                            // Priority 3: System Location Manager Fallback (GPS / Network)
                            trySystemLocationManager(context, callback);
                        }
                    })
                    .addOnFailureListener(e -> trySystemLocationManager(context, callback));
        } catch (Exception e) {
            trySystemLocationManager(context, callback);
        }
    }

    @SuppressLint("MissingPermission")
    private static void trySystemLocationManager(Context context, LocationResultCallback callback) {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location gpsLoc = null;
                Location netLoc = null;

                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                Location bestLocation = null;
                if (gpsLoc != null && netLoc != null) {
                    bestLocation = (gpsLoc.getTime() > netLoc.getTime()) ? gpsLoc : netLoc;
                } else if (gpsLoc != null) {
                    bestLocation = gpsLoc;
                } else if (netLoc != null) {
                    bestLocation = netLoc;
                }

                if (bestLocation != null && isValidLocation(bestLocation)) {
                    Log.d(TAG, "System LocationManager fallback location acquired: " + bestLocation.getLatitude() + ", " + bestLocation.getLongitude());
                    callback.onLocationFetched(bestLocation);
                    return;
                }

                // Priority 4: Request a single fresh update via Fused Location Request
                requestSingleFreshUpdate(context, callback);
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying System LocationManager", e);
        }

        callback.onError("Unable to obtain GPS fix. Please ensure you are outdoors or GPS is active.");
    }

    @SuppressLint("MissingPermission")
    private static void requestSingleFreshUpdate(Context context, LocationResultCallback callback) {
        try {
            FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(context);
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                    .setMaxUpdates(1)
                    .setDurationMillis(8000)
                    .build();

            fusedClient.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    fusedClient.removeLocationUpdates(this);
                    Location loc = locationResult.getLastLocation();
                    if (loc != null && isValidLocation(loc)) {
                        callback.onLocationFetched(loc);
                    } else {
                        callback.onError("GPS fix timeout. Please try tapping on the map or retry in an open area.");
                    }
                }
            }, Looper.getMainLooper());
        } catch (Exception e) {
            callback.onError("Failed to acquire fresh GPS signal: " + e.getMessage());
        }
    }

    private static boolean isValidLocation(Location location) {
        return location != null && (location.getLatitude() != 0.0 || location.getLongitude() != 0.0);
    }
}
