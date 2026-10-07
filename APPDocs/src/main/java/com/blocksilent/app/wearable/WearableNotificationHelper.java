package com.blocksilent.app.wearable;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.blocksilent.app.R;
import com.blocksilent.app.activities.MainActivity;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.WearableSettingsEntity;

public class WearableNotificationHelper {
    private static final String TAG = "BLOCKSILENT_WEAR";
    public static final String WEAR_CHANNEL_ID = "blocksilent_wearable_channel";
    public static final String EMERGENCY_CHANNEL_ID = "blocksilent_emergency_channel";

    private final Context context;
    private final NotificationManager notificationManager;

    public WearableNotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel wearChannel = new NotificationChannel(
                    WEAR_CHANNEL_ID,
                    "BlockSilent Wearable & Zone Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            wearChannel.setDescription("Haptic synchronized alerts for smartwatches and wearables.");
            wearChannel.enableVibration(true);
            wearChannel.setVibrationPattern(new long[]{0, 150, 100, 150});

            NotificationChannel emergencyChannel = new NotificationChannel(
                    EMERGENCY_CHANNEL_ID,
                    "BlockSilent Emergency Break-Glass Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            emergencyChannel.setDescription("Critical alerts and siren triggers.");
            emergencyChannel.enableVibration(true);
            emergencyChannel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(wearChannel);
                notificationManager.createNotificationChannel(emergencyChannel);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void notifyZoneTransition(String title, String message, String transitionType) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            WearableSettingsEntity settings = db.wearableSettingsDao().getWearableSettingsSync();
            if (settings != null && !settings.isWearableSyncEnabled()) {
                return;
            }

            long[] vibrationPattern;
            if ("EXIT".equalsIgnoreCase(transitionType)) {
                vibrationPattern = new long[]{0, 300, 150, 150};
            } else {
                vibrationPattern = new long[]{0, 150, 100, 150};
            }

            triggerHaptic(vibrationPattern);

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender()
                    .setHintHideIcon(false)
                    .setBackground(null);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WEAR_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVibrate(vibrationPattern)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .extend(wearableExtender);

            try {
                NotificationManagerCompat.from(context).notify(1002, builder.build());
            } catch (Exception e) {
                Log.w(TAG, "Failed sending wearable notification: " + e.getMessage());
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void notifyEmergencyAlert(String contactName, String reason) {
        long[] emergencyVibe = new long[]{0, 500, 200, 500, 200, 500};
        triggerHaptic(emergencyVibe);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, EMERGENCY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🚨 EMERGENCY BREAK-GLASS ALERT")
                .setContentText("Emergency trigger from " + contactName + ": " + reason)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Emergency trigger received from " + contactName + ". Silent mode cancelled and audible alarm activated.\n" + reason))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(emergencyVibe)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .extend(wearableExtender);

        try {
            NotificationManagerCompat.from(context).notify(1003, builder.build());
        } catch (Exception e) {
            Log.w(TAG, "Failed sending emergency notification: " + e.getMessage());
        }
    }

    private void triggerHaptic(long[] pattern) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    Vibrator vibrator = vibratorManager.getDefaultVibrator();
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                }
            } else {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                    } else {
                        vibrator.vibrate(pattern, -1);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Haptic vibration failed: " + e.getMessage());
        }
    }
}
