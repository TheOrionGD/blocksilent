package com.blocksilent.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.blocksilent.app.R;
import com.blocksilent.app.activities.MainActivity;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.geofence.GeofenceManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchAutomation, switchNotifications;
    private Button btnSettingsWifiSurvey, btnSettingsSensors, btnSettingsEmergency, btnSettingsNoise, btnSettingsWearables;
    private Button btnSettingsDiagnostics, btnSettingsBatteryOptimization, btnSettingsPrivacyPolicy, btnClearAllData;

    private AppDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Context ctx = requireContext();
        database = AppDatabase.getInstance(ctx.getApplicationContext());

        switchAutomation = view.findViewById(R.id.switchSettingsAutomation);
        switchNotifications = view.findViewById(R.id.switchSettingsNotifications);

        btnSettingsWifiSurvey = view.findViewById(R.id.btnSettingsWifiSurvey);
        btnSettingsSensors = view.findViewById(R.id.btnSettingsSensors);
        btnSettingsEmergency = view.findViewById(R.id.btnSettingsEmergency);
        btnSettingsNoise = view.findViewById(R.id.btnSettingsNoise);
        btnSettingsWearables = view.findViewById(R.id.btnSettingsWearables);

        btnSettingsDiagnostics = view.findViewById(R.id.btnSettingsDiagnostics);
        btnSettingsBatteryOptimization = view.findViewById(R.id.btnSettingsBatteryOptimization);
        btnSettingsPrivacyPolicy = view.findViewById(R.id.btnSettingsPrivacyPolicy);
        btnClearAllData = view.findViewById(R.id.btnClearAllData);

        setupListeners();
        loadSettingsFromDb();

        return view;
    }

    private void loadSettingsFromDb() {
        database.settingsDao().getSettings().observe(getViewLifecycleOwner(), settings -> {
            if (!isAdded() || settings == null) return;
            switchAutomation.setOnCheckedChangeListener(null);
            switchNotifications.setOnCheckedChangeListener(null);

            switchAutomation.setChecked(settings.isAutomationEnabled());
            switchNotifications.setChecked(settings.isNotificationsEnabled());

            switchAutomation.setOnCheckedChangeListener((btn, isChecked) -> {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    settings.setAutomationEnabled(isChecked);
                    database.settingsDao().insertOrUpdate(settings);
                });
            });

            switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    settings.setNotificationsEnabled(isChecked);
                    database.settingsDao().insertOrUpdate(settings);
                });
            });
        });
    }

    private void setupListeners() {
        btnSettingsWifiSurvey.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new WifiSurveyFragment());
            }
        });

        btnSettingsSensors.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new SensorSettingsFragment());
            }
        });

        btnSettingsEmergency.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new EmergencySettingsFragment());
            }
        });

        btnSettingsNoise.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new NoiseDetectionFragment());
            }
        });

        btnSettingsWearables.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new WearableSettingsFragment());
            }
        });

        btnSettingsDiagnostics.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new DiagnosticsFragment());
            }
        });

        btnSettingsBatteryOptimization.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new BatteryOptimizationFragment());
            }
        });

        btnSettingsPrivacyPolicy.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new PrivacyFragment());
            }
        });

        btnClearAllData.setOnClickListener(v -> showClearAllDataConfirmation());
    }

    private void showClearAllDataConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete All Local Data?")
                .setMessage("This will permanently delete all saved blocks, Wi-Fi mappings, sensor configurations, VIP contacts, timetables, and audit history from your device. Are you sure?")
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    Context appContext = requireContext().getApplicationContext();
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        GeofenceManager geofenceManager = new GeofenceManager(appContext);
                        geofenceManager.unregisterAllGeofences();

                        if (database != null) {
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
                            database.settingsDao().insertOrUpdate(new SettingsEntity(true, true, "NORMAL", 0, false));
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "All local data reset.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
