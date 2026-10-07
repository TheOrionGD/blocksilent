package com.blocksilent.app.emergency;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.EmergencyContactEntity;
import com.blocksilent.app.database.entities.EmergencyEventEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.notifications.NotificationHelper;
import com.blocksilent.app.utils.SoundModeManager;
import com.blocksilent.app.wearable.WearableNotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmergencyManager {
    private static final String TAG = "BLOCKSILENT_EMERGENCY";
    private static final long COOLDOWN_DURATION_MS = 15 * 60 * 1000L; // 15 minutes per contact cooldown

    public interface EmergencyStateListener {
        void onEmergencyTriggered(String contactName, String source);
        void onEmergencyAcknowledged();
    }

    private final Context context;
    private final AppDatabase database;
    private final SoundModeManager soundModeManager;
    private final NotificationHelper notificationHelper;
    private final WearableNotificationHelper wearableHelper;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MediaPlayer sirenPlayer;
    private boolean isEmergencyActive = false;
    private EmergencyStateListener listener;

    public EmergencyManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.context);
        this.soundModeManager = new SoundModeManager(this.context);
        this.notificationHelper = new NotificationHelper(this.context);
        this.wearableHelper = new WearableNotificationHelper(this.context);
    }

    public void setListener(EmergencyStateListener listener) {
        this.listener = listener;
    }

    public boolean isEmergencyActive() {
        return isEmergencyActive;
    }

    /**
     * Trigger Emergency Break-Glass from VIP contact via keyword or manual action.
     */
    public synchronized void triggerEmergencyOverride(String contactPhone, String source, boolean startSiren) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            String formattedTime = new SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault()).format(new Date());

            EmergencyContactEntity contact = database.emergencyContactDao().findByPhoneSync(contactPhone);
            String contactName = contact != null ? contact.getName() : (contactPhone != null ? contactPhone : "Emergency User");

            // Check anti-loop cooldown
            if (contact != null) {
                if (now - contact.getLastTriggerTimestamp() < COOLDOWN_DURATION_MS) {
                    Log.w(TAG, "Emergency trigger rejected for " + contactName + " due to active anti-SMS loop cooldown.");
                    return;
                }
                database.emergencyContactDao().updateLastTriggerTimestamp(contact.getId(), now);
            }

            // Record event in Room
            EmergencyEventEntity event = new EmergencyEventEntity(
                    now, formattedTime, contactName, contactPhone, source,
                    "Emergency Break-Glass triggered. Silent mode cancelled.", startSiren
            );
            database.emergencyEventDao().insert(event);

            // Set emergency override flag in settings (e.g. 2 hours temporary safety)
            SettingsEntity settings = database.settingsDao().getSettingsSync();
            if (settings != null) {
                settings.setOverrideUntilTimestamp(now + (2 * 60 * 60 * 1000L)); // 2 hours
                database.settingsDao().insertOrUpdate(settings);
            }

            // Immediately restore NORMAL audible ringer mode with high volume
            soundModeManager.applySoundModeWithResult(SoundModeManager.MODE_NORMAL);
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                try {
                    int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVol, 0);
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0);
                } catch (Exception ignored) {}
            }

            isEmergencyActive = true;

            // Wearable & notifications
            wearableHelper.notifyEmergencyAlert(contactName, "Silent mode overridden by " + source);

            mainHandler.post(() -> {
                if (startSiren) {
                    playEmergencySiren();
                }
                if (listener != null) {
                    listener.onEmergencyTriggered(contactName, source);
                }
            });

            Log.i(TAG, "🚨 CRITICAL EMERGENCY BREAK-GLASS ACTIVATED BY " + contactName + " (" + source + ")");
        });
    }

    public synchronized void acknowledgeAndStopEmergency() {
        isEmergencyActive = false;
        stopSiren();
        if (listener != null) {
            listener.onEmergencyAcknowledged();
        }
        Log.d(TAG, "Emergency acknowledged and audible siren stopped.");
    }

    private void playEmergencySiren() {
        stopSiren();
        try {
            Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            sirenPlayer = new MediaPlayer();
            sirenPlayer.setDataSource(context, alertUri);
            sirenPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );
            sirenPlayer.setLooping(true);
            sirenPlayer.prepare();
            sirenPlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed playing emergency siren: " + e.getMessage());
        }
    }

    private void stopSiren() {
        if (sirenPlayer != null) {
            try {
                if (sirenPlayer.isPlaying()) {
                    sirenPlayer.stop();
                }
                sirenPlayer.release();
            } catch (Exception ignored) {}
            sirenPlayer = null;
        }
    }

    public void sendMissedCallStatusSms(String toPhoneNumber, String locationName, String untilTime) {
        if (toPhoneNumber == null || toPhoneNumber.isEmpty()) return;
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = "I'm currently in " + (locationName != null ? locationName : "Class/Lab")
                    + ". My phone is silenced until " + (untilTime != null ? untilTime : "later")
                    + ". For an urgent emergency, reply EMERGENCY.";
            smsManager.sendTextMessage(toPhoneNumber, null, message, null, null);
            Log.d(TAG, "Automated Emergency Status SMS sent to " + toPhoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send emergency SMS: " + e.getMessage());
        }
    }
}
