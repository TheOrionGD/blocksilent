package com.blocksilent.app.emergency;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.EmergencyContactEntity;
import com.blocksilent.app.utils.SoundModeManager;

public class CallStateReceiver extends BroadcastReceiver {
    private static final String TAG = "BLOCKSILENT_CALL";
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static String incomingNumber = null;
    private static boolean isMissed = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            return;
        }

        String stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        if (number != null) {
            incomingNumber = number.replaceAll("[^0-9+]", "");
        }

        int state = TelephonyManager.CALL_STATE_IDLE;
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(stateStr)) {
            state = TelephonyManager.CALL_STATE_RINGING;
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(stateStr)) {
            state = TelephonyManager.CALL_STATE_OFFHOOK;
        }

        onCustomCallStateChanged(context, state, incomingNumber);
    }

    private void onCustomCallStateChanged(Context context, int state, String number) {
        if (lastState == state) {
            return;
        }

        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isMissed = true;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                isMissed = false;
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (lastState == TelephonyManager.CALL_STATE_RINGING && isMissed && number != null && !number.isEmpty()) {
                    Log.d(TAG, "Missed call detected from: " + number);
                    handleMissedCall(context, number);
                }
                isMissed = false;
                break;
        }
        lastState = state;
    }

    private void handleMissedCall(Context context, String phone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            EmergencyContactEntity contact = db.emergencyContactDao().findByPhoneSync(phone);
            if (contact != null && contact.isSmsEnabled()) {
                SoundModeManager soundModeManager = new SoundModeManager(context);
                String currentRinger = soundModeManager.getCurrentRingerMode();
                if (SoundModeManager.MODE_SILENT.equalsIgnoreCase(currentRinger) || SoundModeManager.MODE_VIBRATE.equalsIgnoreCase(currentRinger)) {
                    Log.i(TAG, "Sending automated missed call emergency status SMS to VIP contact: " + contact.getName());
                    EmergencyManager emergencyManager = new EmergencyManager(context);
                    emergencyManager.sendMissedCallStatusSms(phone, "Campus Silent Zone", "class completes");
                }
            }
        });
    }
}
