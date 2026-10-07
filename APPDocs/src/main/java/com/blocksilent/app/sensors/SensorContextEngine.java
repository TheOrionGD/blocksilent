package com.blocksilent.app.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.SensorSettingsEntity;

public class SensorContextEngine implements SensorEventListener {
    private static final String TAG = "BLOCKSILENT_SENSOR";

    public interface SensorStateListener {
        void onFaceDownSilenceStateChanged(boolean isFaceDownSilenced);
    }

    private final Context context;
    private final SensorManager sensorManager;
    private final AppDatabase database;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Sensor accelerometerSensor;
    private Sensor proximitySensor;
    private Sensor lightSensor;

    private boolean isProximityNear = false;
    private float currentLux = 100.0f;
    private boolean isOrientationFaceDown = false;
    private boolean isCurrentlyFaceDownSilenced = false;

    private SensorStateListener listener;
    private Runnable confirmationRunnable;
    private Runnable pickupRunnable;
    private boolean isMonitoring = false;

    public SensorContextEngine(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        this.database = AppDatabase.getInstance(this.context);

        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
    }

    public void setListener(SensorStateListener listener) {
        this.listener = listener;
    }

    public synchronized void startMonitoring() {
        if (isMonitoring || sensorManager == null) return;

        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }

        isMonitoring = true;
        Log.d(TAG, "Sensor context engine monitoring started.");
    }

    public synchronized void stopMonitoring() {
        if (!isMonitoring || sensorManager == null) return;
        sensorManager.unregisterListener(this);
        isMonitoring = false;
        cancelConfirmation();
        cancelPickup();
        Log.d(TAG, "Sensor context engine monitoring stopped.");
    }

    public boolean isCurrentlyFaceDownSilenced() {
        return isCurrentlyFaceDownSilenced;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;

        int sensorType = event.sensor.getType();
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            handleAccelerometer(event.values);
        } else if (sensorType == Sensor.TYPE_PROXIMITY) {
            handleProximity(event.values);
        } else if (sensorType == Sensor.TYPE_LIGHT) {
            handleLight(event.values);
        }
    }

    private void handleAccelerometer(float[] values) {
        if (values == null || values.length < 3) return;
        float x = values[0];
        float y = values[1];
        float z = values[2];

        // Device is lying flat face down when Z axis is significantly negative (gravity points up towards back of device)
        // and X/Y tilt is relatively low.
        boolean faceDown = (z < -7.0f) && (Math.abs(x) < 4.5f) && (Math.abs(y) < 4.5f);
        if (faceDown != isOrientationFaceDown) {
            isOrientationFaceDown = faceDown;
            evaluateSensorCondition();
        }
    }

    private void handleProximity(float[] values) {
        if (values == null || values.length == 0) return;
        float distance = values[0];
        float maxRange = proximitySensor != null ? proximitySensor.getMaximumRange() : 5.0f;
        boolean near = (distance < 2.0f) || (distance < maxRange);
        if (near != isProximityNear) {
            isProximityNear = near;
            evaluateSensorCondition();
        }
    }

    private void handleLight(float[] values) {
        if (values == null || values.length == 0) return;
        currentLux = values[0];
        // Low light check is evaluated in evaluateSensorCondition
    }

    private void evaluateSensorCondition() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            SensorSettingsEntity settings = database.sensorSettingsDao().getSensorSettingsSync();
            if (settings == null) {
                settings = new SensorSettingsEntity();
            }

            if (!settings.isFlipToSilenceEnabled()) {
                if (isCurrentlyFaceDownSilenced) {
                    resetSilenceState();
                }
                return;
            }

            boolean conditionMet = isOrientationFaceDown;

            if (settings.isRequireProximity() && !isProximityNear) {
                conditionMet = false;
            }

            if (settings.isRequireLowLight() && currentLux > 25.0f) {
                conditionMet = false;
            }

            final boolean finalConditionMet = conditionMet;
            final long confirmDuration = settings.getConfirmationDurationMs();
            final boolean pickupRestoreEnabled = settings.isPickupRestoreEnabled();

            mainHandler.post(() -> {
                if (finalConditionMet) {
                    cancelPickup();
                    if (!isCurrentlyFaceDownSilenced && confirmationRunnable == null) {
                        confirmationRunnable = () -> {
                            isCurrentlyFaceDownSilenced = true;
                            confirmationRunnable = null;
                            Log.d(TAG, "Flip-to-Silence ACTIVATED after confirmation period.");
                            if (listener != null) {
                                listener.onFaceDownSilenceStateChanged(true);
                            }
                        };
                        mainHandler.postDelayed(confirmationRunnable, confirmDuration);
                    }
                } else {
                    cancelConfirmation();
                    if (isCurrentlyFaceDownSilenced && pickupRestoreEnabled) {
                        if (pickupRunnable == null) {
                            pickupRunnable = () -> {
                                isCurrentlyFaceDownSilenced = false;
                                pickupRunnable = null;
                                Log.d(TAG, "Pickup detected: Flip-to-Silence RESTORED.");
                                if (listener != null) {
                                    listener.onFaceDownSilenceStateChanged(false);
                                }
                            };
                            mainHandler.postDelayed(pickupRunnable, 500); // 500ms debounce for pickup
                        }
                    }
                }
            });
        });
    }

    private void resetSilenceState() {
        mainHandler.post(() -> {
            isCurrentlyFaceDownSilenced = false;
            if (listener != null) {
                listener.onFaceDownSilenceStateChanged(false);
            }
        });
    }

    private void cancelConfirmation() {
        if (confirmationRunnable != null) {
            mainHandler.removeCallbacks(confirmationRunnable);
            confirmationRunnable = null;
        }
    }

    private void cancelPickup() {
        if (pickupRunnable != null) {
            mainHandler.removeCallbacks(pickupRunnable);
            pickupRunnable = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
