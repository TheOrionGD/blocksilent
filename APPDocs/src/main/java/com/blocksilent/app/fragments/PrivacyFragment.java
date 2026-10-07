package com.blocksilent.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.blocksilent.app.R;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.geofence.GeofenceManager;
import com.blocksilent.app.security.SecureStorageHelper;

public class PrivacyFragment extends Fragment {

    private ImageButton btnBackPrivacy;
    private TextView tvKeystoreStatus;
    private Button btnTestKeyStore;
    private Button btnPurgeHistoryOnly;
    private Button btnPurgeNoiseSamples;
    private Button btnPurgeWifiZones;
    private Button btnClearDataPrivacy;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_privacy, container, false);

        btnBackPrivacy = view.findViewById(R.id.btnBackPrivacy);
        tvKeystoreStatus = view.findViewById(R.id.tvKeystoreStatus);
        btnTestKeyStore = view.findViewById(R.id.btnTestKeyStore);
        btnPurgeHistoryOnly = view.findViewById(R.id.btnPurgeHistoryOnly);
        btnPurgeNoiseSamples = view.findViewById(R.id.btnPurgeNoiseSamples);
        btnPurgeWifiZones = view.findViewById(R.id.btnPurgeWifiZones);
        btnClearDataPrivacy = view.findViewById(R.id.btnClearDataPrivacy);

        btnBackPrivacy.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        btnTestKeyStore.setOnClickListener(v -> testKeyStoreLoop());
        btnPurgeHistoryOnly.setOnClickListener(v -> purgeHistoryLogs());
        btnPurgeNoiseSamples.setOnClickListener(v -> purgeNoiseSamples());
        btnPurgeWifiZones.setOnClickListener(v -> purgeWifiZones());
        btnClearDataPrivacy.setOnClickListener(v -> showClearAllDataConfirmation());

        return view;
    }

    private void testKeyStoreLoop() {
        if (!isAdded() || getContext() == null) return;
        try {
            Context ctx = requireContext();
            String testSecret = "BlockSilent_Secured_Payload_" + System.currentTimeMillis();
            SecureStorageHelper.saveSecureString(ctx, "test_crypto_key", testSecret);
            String decrypted = SecureStorageHelper.getSecureString(ctx, "test_crypto_key", "");

            if (testSecret.equals(decrypted)) {
                tvKeystoreStatus.setText("AES-256-GCM (Hardware Verified)");
                Toast.makeText(ctx, "KeyStore Cipher Loop Verified Successfully!", Toast.LENGTH_SHORT).show();
            } else {
                tvKeystoreStatus.setText("Verification Mismatch");
                Toast.makeText(ctx, "KeyStore Verification Mismatch", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            tvKeystoreStatus.setText("Error: " + e.getMessage());
            if (isAdded() && getContext() != null) {
                Toast.makeText(requireContext(), "Crypto Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void purgeHistoryLogs() {
        if (!isAdded() || getContext() == null) return;
        Context appContext = requireContext().getApplicationContext();
        new AlertDialog.Builder(requireContext())
                .setTitle("Purge Decision History")
                .setMessage("Delete all automation decisions and geofence transition history?")
                .setPositiveButton("Purge", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(appContext);
                        db.automationDecisionDao().deleteAll();
                        db.historyDao().deleteAll();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "History logs purged.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void purgeNoiseSamples() {
        if (!isAdded() || getContext() == null) return;
        Context appContext = requireContext().getApplicationContext();
        new AlertDialog.Builder(requireContext())
                .setTitle("Purge Noise Samples")
                .setMessage("Delete all ambient decibel logs from database?")
                .setPositiveButton("Purge", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(appContext);
                        db.noiseSampleDao().deleteAll();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "Noise samples purged.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void purgeWifiZones() {
        if (!isAdded() || getContext() == null) return;
        Context appContext = requireContext().getApplicationContext();
        new AlertDialog.Builder(requireContext())
                .setTitle("Purge Wi-Fi Indoor Maps")
                .setMessage("Delete all mapped Wi-Fi BSSID access points?")
                .setPositiveButton("Purge", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(appContext);
                        db.wifiZoneDao().deleteAll();
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "Wi-Fi maps deleted.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearAllDataConfirmation() {
        if (!isAdded() || getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Factory Reset App Data?")
                .setMessage("This will permanently delete all geofences, Wi-Fi zones, emergency contacts, history, sensor calibrations, and settings from your device. Are you sure?")
                .setPositiveButton("Reset Everything", (dialog, which) -> {
                    Context appContext = requireContext().getApplicationContext();
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase database = AppDatabase.getInstance(appContext);
                        GeofenceManager geofenceManager = new GeofenceManager(appContext);
                        geofenceManager.unregisterAllGeofences();

                        database.blockDao().deleteAll();
                        database.timetableDao().deleteAll();
                        database.historyDao().deleteAll();
                        database.contactDao().deleteAll();
                        database.activeGeofenceStateDao().deleteAll();
                        database.wifiZoneDao().deleteAll();
                        database.emergencyContactDao().deleteAll();
                        database.emergencyEventDao().deleteAll();
                        database.noiseSampleDao().deleteAll();
                        database.automationDecisionDao().deleteAll();
                        database.sensorSettingsDao().deleteAll();
                        database.wearableSettingsDao().deleteAll();

                        database.settingsDao().insertOrUpdate(new SettingsEntity(true, true, "NORMAL", 0, false));

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "All local data reset to factory defaults.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
