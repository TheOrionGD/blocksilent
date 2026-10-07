package com.blocksilent.app.noise;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.NoiseSampleEntity;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class NoiseDetector {
    private static final String TAG = "BLOCKSILENT_NOISE";
    private static final int SAMPLE_RATE = 44100;

    public interface NoiseSampleCallback {
        void onNoiseMeasured(double approximateDb, String category);
    }

    private final Context context;
    private final AppDatabase database;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isMeasuring = false;

    public NoiseDetector(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(this.context);
    }

    public static String classifyNoise(double dB) {
        if (dB < 40.0) {
            return "Quiet Study Zone";
        } else if (dB <= 60.0) {
            return "Moderate Environment";
        } else {
            return "High Noise / Active Zone";
        }
    }

    public static String getCategoryKey(double dB) {
        if (dB < 40.0) return "QUIET";
        if (dB <= 60.0) return "MODERATE";
        return "HIGH";
    }

    @SuppressLint("MissingPermission")
    public void sampleAmbientNoiseOnce(long blockId, String zoneName, NoiseSampleCallback callback) {
        if (isMeasuring) return;
        isMeasuring = true;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            int minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );

            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                minBufferSize = SAMPLE_RATE * 2;
            }

            AudioRecord audioRecord = null;
            short[] buffer = new short[minBufferSize];
            double computedDb = 35.0; // default baseline

            try {
                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSize
                );

                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.startRecording();
                    int readSize = audioRecord.read(buffer, 0, minBufferSize);

                    if (readSize > 0) {
                        double sum = 0.0;
                        for (int i = 0; i < readSize; i++) {
                            sum += buffer[i] * buffer[i];
                        }
                        double rms = Math.sqrt(sum / readSize);
                        if (rms > 0) {
                            // SPL calculation formula with calibration factor for standard mobile mics
                            computedDb = 20.0 * Math.log10(rms);
                            // Normalize to typical ~30 dB to 95 dB ambient scale
                            if (computedDb < 30.0) computedDb = 30.0;
                            if (computedDb > 100.0) computedDb = 100.0;
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Audio recording sample failed: " + e.getMessage());
            } finally {
                // PRIVACY REQUIREMENT: Immediately zero and discard memory buffer
                Arrays.fill(buffer, (short) 0);
                if (audioRecord != null) {
                    try {
                        audioRecord.stop();
                        audioRecord.release();
                    } catch (Exception ignored) {}
                }
                isMeasuring = false;
            }

            final double finalDb = Math.round(computedDb * 10.0) / 10.0;
            final String category = classifyNoise(finalDb);
            final String catKey = getCategoryKey(finalDb);

            // Record in Room database for campus heatmap / history
            long now = System.currentTimeMillis();
            String formatted = new SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault()).format(new Date());
            NoiseSampleEntity sampleEntity = new NoiseSampleEntity(
                    blockId, zoneName != null ? zoneName : "General Area",
                    finalDb, catKey, now, formatted
            );
            database.noiseSampleDao().insert(sampleEntity);

            if (callback != null) {
                mainHandler.post(() -> callback.onNoiseMeasured(finalDb, category));
            }
        });
    }
}
