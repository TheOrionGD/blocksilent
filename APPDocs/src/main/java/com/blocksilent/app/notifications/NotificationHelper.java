package com.blocksilent.app.notifications;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.blocksilent.app.R;
import com.blocksilent.app.activities.MainActivity;

public class NotificationHelper {
    private static final String TAG = "BLOCKSILENT_BACKGROUND";
    public static final String CHANNEL_ID = "blocksilent_notifications";
    public static final String CHANNEL_NAME = "BlockSilent Status Alerts";
    private static final int NOTIFICATION_ID = 1001;

    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for location-based sound mode changes.");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void sendNotification(String title, String message) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Notification sent: [" + title + "] " + message);
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission not granted, skipping notification: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to send notification", e);
        }
    }

    public void showEnterBlockNotification(String blockName, String previousMode, String newMode) {
        String title;
        if ("SILENT".equalsIgnoreCase(newMode)) {
            title = "Silent Mode Activated";
        } else if ("VIBRATE".equalsIgnoreCase(newMode)) {
            title = "Vibrate Mode Activated";
        } else {
            title = "Normal Sound Mode";
        }
        String message = "You entered " + blockName + ". Sound mode changed from " + previousMode + " to " + newMode + ".";
        sendNotification(title, message);
    }

    public void showEnterBlockNotification(String blockName, String soundMode) {
        showEnterBlockNotification(blockName, "Previous", soundMode);
    }

    public void showExitBlockNotification(String blockName, String restoredMode) {
        String title = "Sound Mode Restored";
        String message = "You left " + blockName + ". Sound mode restored to " + restoredMode + ".";
        sendNotification(title, message);
    }

    public void showSoundChangeFailedNotification(String reason) {
        String title = "Unable to change sound mode";
        String message = "Android did not allow the requested sound-mode change. " + (reason != null ? reason : "Check Notification Policy Access.");
        sendNotification(title, message);
    }

    public void showAutomationDisabledNotification() {
        sendNotification("BlockSilent Automation Disabled", "Emergency override is currently active.");
    }

    public void showAutomationEnabledNotification() {
        sendNotification("BlockSilent Automation Enabled", "Automatic block detection resumed.");
    }
}
