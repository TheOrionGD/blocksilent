package com.blocksilent.app.utils;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class BatteryOptimizationHelper {
    private static final String TAG = "BLOCKSILENT_BACKGROUND";

    public static boolean isBatteryOptimizationIgnored(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (powerManager != null) {
                    boolean isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
                    Log.d(TAG, "Battery optimization ignored for " + context.getPackageName() + ": " + isIgnoring);
                    return isIgnoring;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking battery optimization status", e);
            }
        }
        return true;
    }

    @SuppressLint("BatteryLife")
    public static void requestIgnoreBatteryOptimization(Context context) {
        if (context == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    Intent directIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    directIntent.setData(Uri.parse("package:" + packageName));
                    directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(directIntent);
                    Log.d(TAG, "Launched ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS for " + packageName);
                    return;
                } catch (Exception e) {
                    Log.w(TAG, "Direct request ignore failed, trying battery optimization list settings: " + e.getMessage());
                }
            }

            // Fallback 1: System Battery Optimization List
            try {
                Intent listIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                listIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(listIntent);
                Log.d(TAG, "Launched ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS");
                return;
            } catch (Exception e) {
                Log.w(TAG, "Ignore battery optimization settings failed, trying app details: " + e.getMessage());
            }

            // Fallback 2: OEM Auto-Start / Power management intents (Xiaomi, Huawei, Samsung, Oppo, Vivo)
            if (tryLaunchOemBatterySettings(context)) {
                return;
            }

            // Fallback 3: App Details Settings
            openAppSettings(context);
        } else {
            Toast.makeText(context, "Battery optimization is not restricted on this Android version.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void openBatteryOptimizationSettings(Context context) {
        requestIgnoreBatteryOptimization(context);
    }

    public static void openAppSettings(Context context) {
        try {
            Intent appInfoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            appInfoIntent.setData(Uri.parse("package:" + context.getPackageName()));
            appInfoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appInfoIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open application details settings", e);
        }
    }

    private static boolean tryLaunchOemBatterySettings(Context context) {
        Intent[] oemIntents = new Intent[]{
                // Xiaomi / MIUI
                new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")),
                // Huawei / Honor
                new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                new Intent().setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
                // Oppo / ColorOS
                new Intent().setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                new Intent().setComponent(new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
                // Vivo / Funtouch
                new Intent().setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                new Intent().setComponent(new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                // Samsung
                new Intent().setComponent(new ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))
        };

        for (Intent intent : oemIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.d(TAG, "Launched OEM specific battery management intent: " + intent.getComponent());
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
