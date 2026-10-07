package com.blocksilent.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.blocksilent.app.R;
import com.blocksilent.app.activities.AddEditBlockActivity;
import com.blocksilent.app.activities.MainActivity;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.ActiveGeofenceStateEntity;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.HistoryEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.geofence.GeofenceManager;
import com.blocksilent.app.utils.BatteryOptimizationHelper;
import com.blocksilent.app.utils.DemoModeManager;
import com.blocksilent.app.utils.PermissionManager;
import com.blocksilent.app.utils.SoundModeManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvCurrentLocation, tvCurrentSoundMode, tvAutomationStatus, tvGpsStatus, tvActiveRule;
    private TextView tvConfiguredBlocksCount, tvTodayChangesCount;
    private MaterialCardView cardBackgroundLocationWarning, cardDndWarning;
    private Button btnFixBackgroundLocation, btnFixDndAccess, btnViewDiagnostics;
    private Button btnAddBlock, btnViewBlocks, btnTimetable, btnHistory, btnEmergencyOverride;
    private Spinner spinnerDemoBlocks;
    private Button btnDemoEnterSelected, btnDemoExitLocation;

    private SoundModeManager soundModeManager;
    private DemoModeManager demoModeManager;
    private GeofenceManager geofenceManager;
    private AppDatabase database;

    private final List<BlockEntity> currentBlockList = new ArrayList<>();
    private final List<String> currentBlockNames = new ArrayList<>();
    private ArrayAdapter<String> demoSpinnerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        soundModeManager = new SoundModeManager(requireContext());
        demoModeManager = new DemoModeManager(requireContext());
        geofenceManager = new GeofenceManager(requireContext());
        database = AppDatabase.getInstance(requireContext().getApplicationContext());

        tvCurrentLocation = view.findViewById(R.id.tvCurrentLocation);
        tvCurrentSoundMode = view.findViewById(R.id.tvCurrentSoundMode);
        tvAutomationStatus = view.findViewById(R.id.tvAutomationStatus);
        tvGpsStatus = view.findViewById(R.id.tvGpsStatus);
        tvActiveRule = view.findViewById(R.id.tvActiveRule);

        tvConfiguredBlocksCount = view.findViewById(R.id.tvConfiguredBlocksCount);
        tvTodayChangesCount = view.findViewById(R.id.tvTodayChangesCount);

        cardBackgroundLocationWarning = view.findViewById(R.id.cardBackgroundLocationWarning);
        cardDndWarning = view.findViewById(R.id.cardDndWarning);
        btnFixBackgroundLocation = view.findViewById(R.id.btnFixBackgroundLocation);
        btnFixDndAccess = view.findViewById(R.id.btnFixDndAccess);
        btnViewDiagnostics = view.findViewById(R.id.btnViewDiagnostics);

        btnAddBlock = view.findViewById(R.id.btnQuickAddBlock);
        btnViewBlocks = view.findViewById(R.id.btnQuickViewBlocks);
        btnTimetable = view.findViewById(R.id.btnQuickTimetable);
        btnHistory = view.findViewById(R.id.btnQuickHistory);
        btnEmergencyOverride = view.findViewById(R.id.btnEmergencyOverride);

        spinnerDemoBlocks = view.findViewById(R.id.spinnerDemoBlocks);
        btnDemoEnterSelected = view.findViewById(R.id.btnDemoEnterSelected);
        btnDemoExitLocation = view.findViewById(R.id.btnDemoExitLocation);

        demoSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, currentBlockNames);
        spinnerDemoBlocks.setAdapter(demoSpinnerAdapter);

        setupQuickActions();
        setupDataDrivenDemo();
        observeDatabaseEntities();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDashboardData();
    }

    private void observeDatabaseEntities() {
        database.blockDao().getAllBlocks().observe(getViewLifecycleOwner(), blocks -> {
            currentBlockList.clear();
            currentBlockNames.clear();
            if (blocks != null) {
                currentBlockList.addAll(blocks);
                for (BlockEntity b : blocks) {
                    currentBlockNames.add(b.getName() + " (" + b.getSoundMode() + " - " + (int) b.getRadius() + "m)");
                }
            }
            if (currentBlockNames.isEmpty()) {
                currentBlockNames.add("No blocks configured");
            }
            demoSpinnerAdapter.notifyDataSetChanged();
            updateDashboardData();
        });

        database.activeGeofenceStateDao().getInsideStates().observe(getViewLifecycleOwner(), insideStates -> {
            if (insideStates != null && !insideStates.isEmpty()) {
                ActiveGeofenceStateEntity active = insideStates.get(0);
                tvCurrentLocation.setText("Inside " + active.getBlockName());
                tvActiveRule.setText("Active: " + active.getBlockName() + " → " + active.getAppliedSoundMode());
            } else {
                tvCurrentLocation.setText("Outside Configured Area");
                tvActiveRule.setText("Active Rule: Normal Sound");
            }
            updateDashboardData();
        });
    }

    private void updateDashboardData() {
        boolean hasLoc = PermissionManager.hasLocationPermission(requireContext());
        boolean hasBgLoc = PermissionManager.hasBackgroundLocationPermission(requireContext());
        boolean isGpsOn = PermissionManager.isLocationServicesEnabled(requireContext());
        boolean hasDnd = PermissionManager.hasDndPermission(requireContext());
        boolean isBattIgnored = BatteryOptimizationHelper.isBatteryOptimizationIgnored(requireContext());

        // Update Warning Banners
        cardBackgroundLocationWarning.setVisibility(!hasBgLoc ? View.VISIBLE : View.GONE);
        cardDndWarning.setVisibility(!hasDnd ? View.VISIBLE : View.GONE);

        // Automation Status Pill
        boolean isReady = hasLoc && hasBgLoc && isGpsOn;
        if (isReady) {
            tvAutomationStatus.setText("● ACTIVE");
            tvAutomationStatus.setBackgroundResource(R.drawable.bg_badge_active);
            tvAutomationStatus.setTextColor(android.graphics.Color.parseColor("#10B981"));
        } else {
            tvAutomationStatus.setText("● NOT READY");
            tvAutomationStatus.setBackgroundResource(R.drawable.bg_badge_inactive);
            tvAutomationStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"));
        }

        int registeredCount = geofenceManager.getRegisteredGeofencesCount();
        tvGpsStatus.setText("Geofencing: " + (isReady ? (registeredCount + " Active") : "Unavailable"));

        String currentSound = soundModeManager.getCurrentRingerMode();
        tvCurrentSoundMode.setText(currentSound);
        if (SoundModeManager.MODE_SILENT.equalsIgnoreCase(currentSound)) {
            tvCurrentSoundMode.setBackgroundResource(R.drawable.bg_badge_silent);
            tvCurrentSoundMode.setTextColor(android.graphics.Color.parseColor("#8B5CF6"));
        } else if (SoundModeManager.MODE_VIBRATE.equalsIgnoreCase(currentSound)) {
            tvCurrentSoundMode.setBackgroundResource(R.drawable.bg_badge_vibrate);
            tvCurrentSoundMode.setTextColor(android.graphics.Color.parseColor("#06B6D4"));
        } else {
            tvCurrentSoundMode.setBackgroundResource(R.drawable.bg_badge_normal);
            tvCurrentSoundMode.setTextColor(android.graphics.Color.parseColor("#10B981"));
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            int enabledCount = database.blockDao().getEnabledBlocksCountSync();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            int todayChanges = database.historyDao().getEventsCountSinceSync(startOfDay);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvConfiguredBlocksCount.setText(String.valueOf(enabledCount));
                    tvTodayChangesCount.setText(String.valueOf(todayChanges));
                });
            }
        });
    }

    private void setupQuickActions() {
        btnFixBackgroundLocation.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                PermissionManager.requestBackgroundLocationPermission(requireActivity());
            } else {
                PermissionManager.openAppSettings(requireContext());
            }
        });

        btnFixDndAccess.setOnClickListener(v -> PermissionManager.openDndSettings(requireContext()));

        btnViewDiagnostics.setOnClickListener(v -> showDiagnosticsDialog());

        btnAddBlock.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddEditBlockActivity.class)));

        btnViewBlocks.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_blocks);
            }
        });

        btnTimetable.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_timetable);
            }
        });

        btnHistory.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.nav_history);
            }
        });

        btnEmergencyOverride.setOnClickListener(v -> showEmergencyOverrideDialog());
    }

    private void setupDataDrivenDemo() {
        btnDemoEnterSelected.setOnClickListener(v -> {
            if (currentBlockList.isEmpty()) {
                Toast.makeText(requireContext(), "No blocks configured. Add a block first!", Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedPos = spinnerDemoBlocks.getSelectedItemPosition();
            if (selectedPos >= 0 && selectedPos < currentBlockList.size()) {
                BlockEntity selectedBlock = currentBlockList.get(selectedPos);
                demoModeManager.simulateEnterBlock(selectedBlock.getId());
                Toast.makeText(requireContext(), "Simulating ENTER: " + selectedBlock.getName(), Toast.LENGTH_SHORT).show();
                v.postDelayed(this::updateDashboardData, 600);
            }
        });

        btnDemoExitLocation.setOnClickListener(v -> {
            demoModeManager.simulateExitBlock();
            Toast.makeText(requireContext(), "Simulating EXIT: Outside Configured Area", Toast.LENGTH_SHORT).show();
            v.postDelayed(this::updateDashboardData, 600);
        });
    }

    private void showDiagnosticsDialog() {
        View diagView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_diagnostics, null);

        TextView diagLocationPerm = diagView.findViewById(R.id.diagLocationPerm);
        TextView diagBackgroundPerm = diagView.findViewById(R.id.diagBackgroundPerm);
        TextView diagLocationServices = diagView.findViewById(R.id.diagLocationServices);
        TextView diagGeofenceReg = diagView.findViewById(R.id.diagGeofenceReg);
        TextView diagAutomationState = diagView.findViewById(R.id.diagAutomationState);
        TextView diagEmergencyOverride = diagView.findViewById(R.id.diagEmergencyOverride);
        TextView diagDndPolicy = diagView.findViewById(R.id.diagDndPolicy);
        TextView diagBatteryOpt = diagView.findViewById(R.id.diagBatteryOpt);
        TextView diagLastEvent = diagView.findViewById(R.id.diagLastEvent);

        Button btnDiagReRegister = diagView.findViewById(R.id.btnDiagReRegister);
        Button btnDiagOpenBattery = diagView.findViewById(R.id.btnDiagOpenBattery);
        Button btnDiagOpenDnd = diagView.findViewById(R.id.btnDiagOpenDnd);
        Button btnDiagClose = diagView.findViewById(R.id.btnDiagClose);

        boolean loc = PermissionManager.hasLocationPermission(requireContext());
        boolean bgLoc = PermissionManager.hasBackgroundLocationPermission(requireContext());
        boolean gps = PermissionManager.isLocationServicesEnabled(requireContext());
        boolean dnd = PermissionManager.hasDndPermission(requireContext());
        boolean batt = BatteryOptimizationHelper.isBatteryOptimizationIgnored(requireContext());
        int regCount = geofenceManager.getRegisteredGeofencesCount();
        String regStatus = geofenceManager.getRegistrationStatus();

        diagLocationPerm.setText("Location Permission: " + (loc ? "✓ GRANTED" : "✗ DENIED"));
        diagLocationPerm.setTextColor(android.graphics.Color.parseColor(loc ? "#10B981" : "#EF4444"));

        diagBackgroundPerm.setText("Background Location: " + (bgLoc ? "✓ GRANTED (Allow all the time)" : "✗ NOT GRANTED"));
        diagBackgroundPerm.setTextColor(android.graphics.Color.parseColor(bgLoc ? "#10B981" : "#EF4444"));

        diagLocationServices.setText("Location Services (GPS): " + (gps ? "✓ ON" : "✗ OFF"));
        diagLocationServices.setTextColor(android.graphics.Color.parseColor(gps ? "#10B981" : "#EF4444"));

        diagGeofenceReg.setText("Geofencing Registration: " + regStatus + " (" + regCount + " registered)");
        diagDndPolicy.setText("Sound / DND Policy Access: " + (dnd ? "✓ GRANTED" : "⚠️ NOT GRANTED"));
        diagBatteryOpt.setText("Battery Optimization: " + (batt ? "✓ UNRESTRICTED" : "⚠️ RESTRICTED"));

        AppDatabase.databaseWriteExecutor.execute(() -> {
            SettingsEntity settings = database.settingsDao().getSettingsSync();
            boolean autoEnabled = settings != null && settings.isAutomationEnabled();
            boolean overrideActive = settings != null && settings.getOverrideUntilTimestamp() > System.currentTimeMillis();
            List<HistoryEntity> recentHistory = database.historyDao().getRecentHistorySync(1);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    diagAutomationState.setText("Automation Switch: " + (autoEnabled ? "✓ ON" : "OFF"));
                    diagEmergencyOverride.setText("Emergency Override: " + (overrideActive ? "⚠️ ACTIVE" : "OFF"));

                    if (recentHistory != null && !recentHistory.isEmpty()) {
                        HistoryEntity h = recentHistory.get(0);
                        diagLastEvent.setText("Last Event: " + h.getEventType() + " (" + h.getFormattedDateTime() + ") -> " + h.getNewMode());
                    } else {
                        diagLastEvent.setText("Last Event: None recorded yet");
                    }
                });
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(diagView)
                .create();

        btnDiagReRegister.setOnClickListener(v -> {
            geofenceManager.reRegisterAllGeofences();
            Toast.makeText(requireContext(), "Re-registering all enabled geofences...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            updateDashboardData();
        });

        btnDiagOpenBattery.setOnClickListener(v -> {
            BatteryOptimizationHelper.openBatteryOptimizationSettings(requireContext());
            dialog.dismiss();
        });

        btnDiagOpenDnd.setOnClickListener(v -> {
            PermissionManager.openDndSettings(requireContext());
            dialog.dismiss();
        });

        btnDiagClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEmergencyOverrideDialog() {
        String[] options = {"Disable for 1 Hour", "Disable for 2 Hours", "Disable for 4 Hours", "Cancel Override (Resume Automation)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Emergency Override")
                .setItems(options, (dialog, which) -> {
                    long durationMs = 0;
                    if (which == 0) durationMs = 3600000;
                    else if (which == 1) durationMs = 7200000;
                    else if (which == 2) durationMs = 14400000;

                    final long overrideTime = durationMs > 0 ? (System.currentTimeMillis() + durationMs) : 0;

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        SettingsEntity settings = database.settingsDao().getSettingsSync();
                        if (settings != null) {
                            settings.setOverrideUntilTimestamp(overrideTime);
                            database.settingsDao().insertOrUpdate(settings);
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (overrideTime > 0) {
                                    Toast.makeText(requireContext(), "Emergency Override activated!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireContext(), "Emergency Override cancelled. Automation active.", Toast.LENGTH_SHORT).show();
                                }
                                updateDashboardData();
                            });
                        }
                    });
                })
                .show();
    }
}
