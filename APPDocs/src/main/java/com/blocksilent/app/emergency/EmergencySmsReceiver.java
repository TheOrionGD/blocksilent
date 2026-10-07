package com.blocksilent.app.emergency;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.EmergencyContactEntity;

public class EmergencySmsReceiver extends BroadcastReceiver {
    private static final String TAG = "BLOCKSILENT_SMS";
    public static final String EMERGENCY_KEYWORD = "EMERGENCY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        String format = bundle.getString("format");
        if (pdus == null || pdus.length == 0) return;

        for (Object pdu : pdus) {
            SmsMessage message;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                message = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                message = SmsMessage.createFromPdu((byte[]) pdu);
            }

            if (message == null) continue;

            String sender = message.getDisplayOriginatingAddress();
            String body = message.getMessageBody();

            if (sender == null || body == null) continue;

            String normalizedSender = sender.replaceAll("[^0-9+]", "");
            String normalizedBody = body.trim().toUpperCase();

            if (normalizedBody.contains(EMERGENCY_KEYWORD)) {
                Log.d(TAG, "Incoming SMS from " + normalizedSender + " containing keyword EMERGENCY");
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(context);
                    EmergencyContactEntity contact = db.emergencyContactDao().findByPhoneSync(normalizedSender);
                    if (contact != null && contact.isKeywordEnabled()) {
                        Log.i(TAG, "Authorized VIP contact " + contact.getName() + " triggered EMERGENCY keyword!");
                        EmergencyManager emergencyManager = new EmergencyManager(context);
                        emergencyManager.triggerEmergencyOverride(normalizedSender, "VIP SMS: " + body, true);
                    } else {
                        Log.w(TAG, "SMS keyword received from unauthorized or unwhitelisted number: " + normalizedSender);
                    }
                });
            }
        }
    }
}
