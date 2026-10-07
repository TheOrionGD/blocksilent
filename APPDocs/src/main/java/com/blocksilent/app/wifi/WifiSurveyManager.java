package com.blocksilent.app.wifi;

import android.content.Context;

public class WifiSurveyManager {

    private final Context context;
    private final WifiZoneManager wifiZoneManager;

    public WifiSurveyManager(Context context) {
        this.context = context.getApplicationContext();
        this.wifiZoneManager = new WifiZoneManager(this.context);
    }

    public WifiZoneManager.WifiConnectionInfo getSurveyData() {
        return wifiZoneManager.getCurrentWifiInfo();
    }
}
