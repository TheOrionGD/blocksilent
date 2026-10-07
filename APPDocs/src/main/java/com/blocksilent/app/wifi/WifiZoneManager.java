package com.blocksilent.app.wifi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.WifiZoneEntity;

public class WifiZoneManager {
    private static final String TAG = "BLOCKSILENT_WIFI";

    private final Context context;
    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;
    private final AppDatabase database;

    public static class WifiConnectionInfo {
        public final boolean isConnected;
        public final String ssid;
        public final String bssid;
        public final int rssi;

        public WifiConnectionInfo(boolean isConnected, String ssid, String bssid, int rssi) {
            this.isConnected = isConnected;
            this.ssid = ssid != null ? ssid.replace("\"", "") : "<unknown ssid>";
            this.bssid = bssid != null ? bssid.toUpperCase().trim() : "";
            this.rssi = rssi;
        }
    }

    public WifiZoneManager(Context context) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.database = AppDatabase.getInstance(this.context);
    }

    @SuppressLint("MissingPermission")
    public WifiConnectionInfo getCurrentWifiInfo() {
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            return new WifiConnectionInfo(false, null, null, 0);
        }

        try {
            if (connectivityManager != null) {
                Network activeNetwork = connectivityManager.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                    if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        WifiInfo wifiInfo = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (capabilities.getTransportInfo() instanceof WifiInfo) {
                                wifiInfo = (WifiInfo) capabilities.getTransportInfo();
                            }
                        }
                        if (wifiInfo == null) {
                            wifiInfo = wifiManager.getConnectionInfo();
                        }

                        if (wifiInfo != null && wifiInfo.getBSSID() != null && !wifiInfo.getBSSID().equalsIgnoreCase("02:00:00:00:00:00")) {
                            return new WifiConnectionInfo(true, wifiInfo.getSSID(), wifiInfo.getBSSID(), wifiInfo.getRssi());
                        }
                    }
                }
            }

            // Fallback for legacy connections
            WifiInfo legacyInfo = wifiManager.getConnectionInfo();
            if (legacyInfo != null && legacyInfo.getBSSID() != null && !legacyInfo.getBSSID().equalsIgnoreCase("02:00:00:00:00:00")) {
                return new WifiConnectionInfo(true, legacyInfo.getSSID(), legacyInfo.getBSSID(), legacyInfo.getRssi());
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Missing permission to read Wi-Fi BSSID/SSID: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error obtaining current Wi-Fi info: " + e.getMessage());
        }

        return new WifiConnectionInfo(false, null, null, 0);
    }

    /**
     * Check if the currently connected Wi-Fi BSSID matches any mapped Indoor Room/Zone in the database.
     */
    public WifiZoneEntity getActiveMatchedWifiZoneSync() {
        WifiConnectionInfo info = getCurrentWifiInfo();
        if (!info.isConnected || info.bssid.isEmpty()) {
            return null;
        }

        WifiZoneEntity zone = database.wifiZoneDao().findByBssidSync(info.bssid);
        if (zone != null && zone.isEnabled()) {
            // Signal strength quality validation (if configured threshold is set)
            if (zone.getRssiThreshold() < 0 && info.rssi != 0 && info.rssi < zone.getRssiThreshold()) {
                Log.d(TAG, "Wi-Fi zone found (" + zone.getRoomName() + ") but signal " + info.rssi + " dBm below threshold " + zone.getRssiThreshold());
                // Still return zone with medium confidence or log note
            }
            return zone;
        }
        return null;
    }
}
