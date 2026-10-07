package com.blocksilent.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
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
import com.blocksilent.app.utils.LocationHelper;
import com.blocksilent.app.utils.PermissionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchAutomation, switchNotifications;
    private TextView tvLocationPermission, tvGpsStatus, tvDndPermission;
    private Button btnPermissions, btnDnd, btnImportantContacts, btnBatteryOptimization, btnPrivacyPolicy, btnClearAllData;

    private AppDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Context ctx = getContext();
        if (ctx != null) {
            database = AppDatabase.getInstance(ctx.getApplicationContext());
        }

        switchAutomation = view.findViewById(R.id.switchSettingsAutomation);
        switchNotifications = view.findViewById(R.id.switchSettingsNotifications);

        tvLocationPermission = view.findViewById(R.id.tvSettingsLocationPermission);
        tvGpsStatus = view.findViewById(R.id.tvSettingsGpsStatus);
        tvDndPermission = view.findViewById(R.id.tvSettingsDndPermission);

        btnPermissions = view.findViewById(R.id.btnGrantPermissions);
        btnDnd = view.findViewById(R.id.btnGrantDnd);
        btnImportantContacts = view.findViewById(R.id.btnImportantContacts);
        btnBatteryOptimization = view.findViewById(R.id.btnBatteryOptimization);
        btnPrivacyPolicy = view.findViewById(R.id.btnPrivacyPolicy);
        btnClearAllData = view.findViewById(R.id.btnClearAllData);

        setupListeners();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissionStatus();
        loadSettingsFromDb();
    }

    private void updatePermissionStatus() {
        if (!isAdded() || getContext() == null) return;

        boolean locGranted = PermissionManager.hasLocationPermission(requireContext());
        tvLocationPermission.setText("Location Permission: " + (locGranted ? "Granted" : "Denied"));

        boolean gpsOn = LocationHelper.isGpsEnabled(requireContext());
        tvGpsStatus.setText("GPS Status: " + (gpsOn ? "ON" : "OFF"));

        boolean dndGranted = PermissionManager.hasDndPermission(requireContext());
        tvDndPermission.setText("Do Not Disturb Access: " + (dndGranted ? "Granted" : "Not Granted"));
    }

    private void loadSettingsFromDb() {
        if (!isAdded() || database == null) return;
        database.settingsDao().getSettings().observe(getViewLifecycleOwner(), settings -> {
            if (!isAdded()) return;
            if (settings != null) {
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
            }
        });
    }

    private void setupListeners() {
        btnPermissions.setOnClickListener(v -> {
            if (isAdded() && getContext() != null) {
                PermissionManager.openAppSettings(requireContext());
            }
        });

        btnDnd.setOnClickListener(v -> {
            if (isAdded() && getContext() != null) {
                PermissionManager.openDndSettings(requireContext());
            }
        });

        btnImportantContacts.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new ImportantContactsFragment());
            }
        });

        btnBatteryOptimization.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new BatteryOptimizationFragment());
            }
        });

        btnPrivacyPolicy.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new PrivacyFragment());
            }
        });

        btnClearAllData.setOnClickListener(v -> showClearAllDataConfirmation());
    }

    private void showClearAllDataConfirmation() {
        if (!isAdded() || getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete All Local Data?")
                .setMessage("This will permanently delete all saved blocks, timetables, history, and settings from your device. Are you sure?")
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
                            // Reset with default settings to prevent null pointer exceptions
                            database.settingsDao().insertOrUpdate(new SettingsEntity(true, true, "NORMAL", 0, false));
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "All local data reset.", Toast.LENGTH_SHORT).show();
                                    updatePermissionStatus();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
