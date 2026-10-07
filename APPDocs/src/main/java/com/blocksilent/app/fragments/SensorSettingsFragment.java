package com.blocksilent.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blocksilent.app.R;
import com.blocksilent.app.context.ContextEngine;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.SensorSettingsEntity;
import com.blocksilent.app.sensors.SensorContextEngine;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SensorSettingsFragment extends Fragment implements SensorContextEngine.SensorStateListener {

    private TextView tvLiveOrientation, tvLiveSensorSilenceState;
    private SwitchMaterial switchFlipToSilence, switchPickupRestore, switchRequireProximity, switchRequireLowLight;
    private Spinner spinnerConfirmationDuration;

    private AppDatabase database;
    private SensorContextEngine sensorEngine;
    private ContextEngine contextEngine;
    private SensorSettingsEntity currentSettings;

    private final String[] durationLabels = new String[]{"0.5 Seconds", "1.0 Second", "1.5 Seconds", "2.0 Seconds"};
    private final long[] durationValues = new long[]{500L, 1000L, 1500L, 2000L};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensor_settings, container, false);

        Context ctx = requireContext();
        database = AppDatabase.getInstance(ctx.getApplicationContext());
        sensorEngine = new SensorContextEngine(ctx);
        contextEngine = ContextEngine.getInstance(ctx);
        sensorEngine.setListener(this);

        tvLiveOrientation = view.findViewById(R.id.tvLiveOrientation);
        tvLiveSensorSilenceState = view.findViewById(R.id.tvLiveSensorSilenceState);

        view.findViewById(R.id.btnSensorBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        switchFlipToSilence = view.findViewById(R.id.switchFlipToSilence);
        switchPickupRestore = view.findViewById(R.id.switchPickupRestore);
        switchRequireProximity = view.findViewById(R.id.switchRequireProximity);
        switchRequireLowLight = view.findViewById(R.id.switchRequireLowLight);
        spinnerConfirmationDuration = view.findViewById(R.id.spinnerConfirmationDuration);

        setupDurationSpinner();
        loadSettingsFromDb();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorEngine != null) {
            sensorEngine.startMonitoring();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorEngine != null) {
            sensorEngine.stopMonitoring();
        }
    }

    private void setupDurationSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, durationLabels);
        spinnerConfirmationDuration.setAdapter(adapter);

        spinnerConfirmationDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (currentSettings != null && position >= 0 && position < durationValues.length) {
                    long chosenDuration = durationValues[position];
                    if (currentSettings.getConfirmationDurationMs() != chosenDuration) {
                        currentSettings.setConfirmationDurationMs(chosenDuration);
                        saveSettings();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadSettingsFromDb() {
        database.sensorSettingsDao().getSensorSettings().observe(getViewLifecycleOwner(), settings -> {
            if (settings != null) {
                currentSettings = settings;

                switchFlipToSilence.setOnCheckedChangeListener(null);
                switchPickupRestore.setOnCheckedChangeListener(null);
                switchRequireProximity.setOnCheckedChangeListener(null);
                switchRequireLowLight.setOnCheckedChangeListener(null);

                switchFlipToSilence.setChecked(settings.isFlipToSilenceEnabled());
                switchPickupRestore.setChecked(settings.isPickupRestoreEnabled());
                switchRequireProximity.setChecked(settings.isRequireProximity());
                switchRequireLowLight.setChecked(settings.isRequireLowLight());

                // Set spinner selection
                for (int i = 0; i < durationValues.length; i++) {
                    if (durationValues[i] == settings.getConfirmationDurationMs()) {
                        spinnerConfirmationDuration.setSelection(i);
                        break;
                    }
                }

                setupSwitchListeners();
            }
        });
    }

    private void setupSwitchListeners() {
        switchFlipToSilence.setOnCheckedChangeListener((btn, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setFlipToSilenceEnabled(isChecked);
                saveSettings();
            }
        });

        switchPickupRestore.setOnCheckedChangeListener((btn, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setPickupRestoreEnabled(isChecked);
                saveSettings();
            }
        });

        switchRequireProximity.setOnCheckedChangeListener((btn, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setRequireProximity(isChecked);
                saveSettings();
            }
        });

        switchRequireLowLight.setOnCheckedChangeListener((btn, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setRequireLowLight(isChecked);
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (currentSettings != null) {
                database.sensorSettingsDao().insertOrUpdate(currentSettings);
            }
        });
    }

    @Override
    public void onFaceDownSilenceStateChanged(boolean isFaceDownSilenced) {
        if (!isAdded()) return;
        android.app.Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (isAdded()) {
                    if (isFaceDownSilenced) {
                        tvLiveOrientation.setText("Orientation: Face-Down (Table Contact)");
                        tvLiveSensorSilenceState.setText("Silence State: 🔇 ACTIVATED (Muted)");
                        tvLiveSensorSilenceState.setTextColor(0xFFFF5722);
                    } else {
                        tvLiveOrientation.setText("Orientation: Neutral / Face-Up");
                        tvLiveSensorSilenceState.setText("Silence State: Inactive (Normal)");
                        tvLiveSensorSilenceState.setTextColor(0xFF4CAF50);
                    }
                }
            });
        }
        contextEngine.updateSensorSilenceState(isFaceDownSilenced);
    }
}
