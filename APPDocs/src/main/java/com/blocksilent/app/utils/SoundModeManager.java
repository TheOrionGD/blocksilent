package com.blocksilent.app.utils;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

public class SoundModeManager {
    private static final String TAG = "BLOCKSILENT_SOUND";
    private static final String PREFS_NAME = "block_silent_sound_prefs";
    private static final String KEY_PREVIOUS_MODE = "previous_sound_mode";
    private static final String KEY_PREVIOUS_RING_VOL = "previous_ring_volume";
    private static final String KEY_IS_MODE_OVERRIDDEN = "is_mode_overridden";

    public static final String MODE_SILENT = "SILENT";
    public static final String MODE_VIBRATE = "VIBRATE";
    public static final String MODE_NORMAL = "NORMAL";

    private final Context context;
    private final AudioManager audioManager;
    private final NotificationManager notificationManager;
    private final SharedPreferences prefs;

    public SoundModeManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static class SoundChangeResult {
        public final boolean success;
        public final String requestedMode;
        public final String actualMode;
        public final String failureReason;

        public SoundChangeResult(boolean success, String requestedMode, String actualMode, String failureReason) {
            this.success = success;
            this.requestedMode = requestedMode;
            this.actualMode = actualMode;
            this.failureReason = failureReason;
        }
    }

    /**
     * Checks if Do Not Disturb (Notification Policy Access) permission is granted.
     */
    public boolean hasDndPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager != null && notificationManager.isNotificationPolicyAccessGranted();
        }
        return true;
    }

    /**
     * Get current phone ringer mode string ("NORMAL", "VIBRATE", "SILENT").
     */
    public String getCurrentRingerMode() {
        if (audioManager == null) return MODE_NORMAL;
        int mode = audioManager.getRingerMode();
        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                return MODE_SILENT;
            case AudioManager.RINGER_MODE_VIBRATE:
                return MODE_VIBRATE;
            case AudioManager.RINGER_MODE_NORMAL:
            default:
                return MODE_NORMAL;
        }
    }

    /**
     * Alias for backwards compatibility.
     */
    public String getCurrentSoundMode() {
        return getCurrentRingerMode();
    }

    /**
     * Save the current sound mode and volume levels before applying an automated block rule.
     */
    public synchronized void savePreviousMode() {
        if (!prefs.getBoolean(KEY_IS_MODE_OVERRIDDEN, false)) {
            String currentMode = getCurrentRingerMode();
            int currentRingVol = 0;
            if (audioManager != null) {
                try {
                    currentRingVol = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                } catch (Exception ignored) {}
            }

            prefs.edit()
                .putString(KEY_PREVIOUS_MODE, currentMode)
                .putInt(KEY_PREVIOUS_RING_VOL, currentRingVol)
                .putBoolean(KEY_IS_MODE_OVERRIDDEN, true)
                .apply();
            Log.d(TAG, "BLOCKSILENT_SOUND: Saved previous mode=" + currentMode + " (Ring Vol: " + currentRingVol + ")");
        }
    }

    public synchronized void saveCurrentSoundMode() {
        savePreviousMode();
    }

    public String getSavedPreviousSoundMode() {
        return prefs.getString(KEY_PREVIOUS_MODE, MODE_NORMAL);
    }

    /**
     * Set Silent mode with DND safety fallback and verification.
     */
    public SoundChangeResult setSilent() {
        return applySoundModeWithResult(MODE_SILENT);
    }

    /**
     * Set Vibrate mode with verification.
     */
    public SoundChangeResult setVibrate() {
        return applySoundModeWithResult(MODE_VIBRATE);
    }

    /**
     * Set Normal mode with volume restore and verification.
     */
    public SoundChangeResult setNormal() {
        return applySoundModeWithResult(MODE_NORMAL);
    }

    /**
     * Restore the previous mode saved before entering any geofences.
     */
    public SoundChangeResult restorePreviousMode() {
        String prevMode = prefs.getString(KEY_PREVIOUS_MODE, MODE_NORMAL);
        int prevVol = prefs.getInt(KEY_PREVIOUS_RING_VOL, -1);

        Log.d(TAG, "BLOCKSILENT_SOUND: Restoring previous mode: " + prevMode);
        SoundChangeResult result = applySoundModeWithResult(prevMode);

        if (result.success && audioManager != null && MODE_NORMAL.equalsIgnoreCase(prevMode)) {
            try {
                int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                int targetVol = (prevVol > 0) ? prevVol : (int) (maxVol * 0.75);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, targetVol, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetVol, 0);
            } catch (Exception e) {
                Log.w(TAG, "Failed to restore volume level", e);
            }
        }

        prefs.edit().putBoolean(KEY_IS_MODE_OVERRIDDEN, false).apply();
        return result;
    }

    public String restorePreviousSoundMode() {
        SoundChangeResult result = restorePreviousMode();
        return result.actualMode;
    }

    public void unmuteAndRestoreNormal() {
        restorePreviousMode();
    }

    public boolean applySoundMode(String targetMode) {
        SoundChangeResult res = applySoundModeWithResult(targetMode);
        return res.success;
    }

    /**
     * Applies target mode and returns detailed SoundChangeResult with real hardware verification.
     */
    public synchronized SoundChangeResult applySoundModeWithResult(String targetMode) {
        if (audioManager == null) {
            Log.e(TAG, "BLOCKSILENT_SOUND: AudioManager is null");
            return new SoundChangeResult(false, targetMode, MODE_NORMAL, "AudioManager unavailable");
        }

        String beforeMode = getCurrentRingerMode();
        Log.d(TAG, "BLOCKSILENT_SOUND: Previous mode=" + beforeMode + " | Requested mode=" + targetMode);

        try {
            if (MODE_SILENT.equalsIgnoreCase(targetMode)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasDndPermission()) {
                    Log.w(TAG, "BLOCKSILENT_SOUND: ACCESS_NOTIFICATION_POLICY missing for SILENT. Falling back to VIBRATE");
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    String verified = verifyActualModeChange();
                    return new SoundChangeResult(false, targetMode, verified, "Notification Policy Access (DND) not granted. Used VIBRATE fallback.");
                } else {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                }
            } else if (MODE_VIBRATE.equalsIgnoreCase(targetMode)) {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            } else {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }

            String verifiedMode = verifyActualModeChange();
            Log.d(TAG, "BLOCKSILENT_SOUND: Verified mode=" + verifiedMode);

            boolean matched = verifiedMode.equalsIgnoreCase(targetMode);
            return new SoundChangeResult(matched, targetMode, verifiedMode, matched ? null : "Actual mode differs from requested mode");
        } catch (SecurityException se) {
            Log.e(TAG, "BLOCKSILENT_SOUND: SecurityException changing ringer mode: " + se.getMessage());
            return new SoundChangeResult(false, targetMode, getCurrentRingerMode(), "SecurityException: " + se.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "BLOCKSILENT_SOUND: Error applying sound mode", e);
            return new SoundChangeResult(false, targetMode, getCurrentRingerMode(), e.getMessage());
        }
    }

    /**
     * Verifies the actual mode currently active in Android AudioManager.
     */
    public String verifyActualModeChange() {
        return getCurrentRingerMode();
    }
}
