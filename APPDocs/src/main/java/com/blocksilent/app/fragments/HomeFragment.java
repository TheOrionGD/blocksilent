package com.blocksilent.app.fragments;

import android.content.Context;
import android.content.Intent;
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
import com.blocksilent.app.activities.AddEditBlockActivity;
import com.blocksilent.app.activities.MainActivity;
import com.blocksilent.app.context.ContextEngine;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.geofence.GeofenceManager;
import com.blocksilent.app.utils.PermissionManager;
import com.blocksilent.app.utils.SoundModeManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvCurrentLocation, tvCurrentSoundMode, tvAutomationStatus, tvGpsStatus, tvActiveRule;
    private TextView tvBadgeGps;
    private TextView tvConfiguredBlocksCount, tvTodayChangesCount;
    private MaterialCardView cardBackgroundLocationWarning, cardDndWarning;
    private Button btnFixBackgroundLocation, btnFixDndAccess, btnViewDiagnostics;
    private Button btnAddBlock, btnViewBlocks, btnTimetable, btnHistory, btnEmergencyOverride;

    private SoundModeManager soundModeManager;
    private GeofenceManager geofenceManager;
    private ContextEngine contextEngine;
    private AppDatabase database;

    private final List<BlockEntity> currentBlockList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Context ctx = requireContext();
        soundModeManager = new SoundModeManager(ctx);
        geofenceManager = new GeofenceManager(ctx);
        contextEngine = ContextEngine.getInstance(ctx);
        database = AppDatabase.getInstance(ctx.getApplicationContext());

        tvCurrentLocation = view.findViewById(R.id.tvCurrentLocation);
        tvCurrentSoundMode = view.findViewById(R.id.tvCurrentSoundMode);
        tvAutomationStatus = view.findViewById(R.id.tvAutomationStatus);
        tvGpsStatus = view.findViewById(R.id.tvGpsStatus);
        tvActiveRule = view.findViewById(R.id.tvActiveRule);

        tvBadgeGps = view.findViewById(R.id.tvBadgeGps);

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

        setupQuickActions();
        observeDatabaseEntities();
        observeContextEngine();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDashboardData();
    }

    private void observeContextEngine() {
        contextEngine.getLiveContextState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !isAdded()) return;

            tvCurrentLocation.setText(state.getPrimaryLocationName());
            tvActiveRule.setText("Source: " + state.getSource() + " • " + state.getReason());
            tvCurrentSoundMode.setText(state.getRecommendedSoundMode());

            if (SoundModeManager.MODE_SILENT.equalsIgnoreCase(state.getRecommendedSoundMode())) {
                tvCurrentSoundMode.setBackgroundResource(R.drawable.bg_badge_silent);
                tvCurrentSoundMode.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.mode_silent));
            } else if (SoundModeManager.MODE_VIBRATE.equalsIgnoreCase(state.getRecommendedSoundMode())) {
                tvCurrentSoundMode.setBackgroundResource(R.drawable.bg_badge_vibrate);
                tvCurrentSoundMode.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.mode_vibrate));
            } else {
                tvCurrentSoundMode.setBackgroundResource(R.drawable.bg_badge_normal);
                tvCurrentSoundMode.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.mode_normal));
            }

            // GPS Status Badge
            tvBadgeGps.setText(state.getActiveGeofences().isEmpty() ? "📍 Real-Time Location GPS: Outside Campus Blocks" : "📍 Real-Time Location GPS: Inside " + state.getActiveGeofences().get(0).getName());
        });
    }

    private void observeDatabaseEntities() {
        database.blockDao().getAllBlocks().observe(getViewLifecycleOwner(), blocks -> {
            currentBlockList.clear();
            if (blocks != null) {
                currentBlockList.addAll(blocks);
            }
            updateDashboardData();
        });
    }

    private void updateDashboardData() {
        boolean hasLoc = PermissionManager.hasLocationPermission(requireContext());
        boolean hasBgLoc = PermissionManager.hasBackgroundLocationPermission(requireContext());
        boolean isGpsOn = PermissionManager.isLocationServicesEnabled(requireContext());
        boolean hasDnd = PermissionManager.hasDndPermission(requireContext());

        cardBackgroundLocationWarning.setVisibility(!hasBgLoc ? View.VISIBLE : View.GONE);
        cardDndWarning.setVisibility(!hasDnd ? View.VISIBLE : View.GONE);

        boolean isReady = hasLoc && hasBgLoc && isGpsOn;
        if (isReady) {
            tvAutomationStatus.setText("● ACTIVE");
            tvAutomationStatus.setBackgroundResource(R.drawable.bg_badge_active);
            tvAutomationStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_active));
        } else {
            tvAutomationStatus.setText("● NOT READY");
            tvAutomationStatus.setBackgroundResource(R.drawable.bg_badge_inactive);
            tvAutomationStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_inactive));
        }

        int registeredCount = geofenceManager.getRegisteredGeofencesCount();
        tvGpsStatus.setText("Geofencing: " + (isReady ? (registeredCount + " Active Zones") : "Unavailable"));

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
                    if (isAdded()) {
                        tvConfiguredBlocksCount.setText(String.valueOf(enabledCount));
                        tvTodayChangesCount.setText(String.valueOf(todayChanges));
                    }
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

        btnViewDiagnostics.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new DiagnosticsFragment());
            }
        });

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

    private void showEmergencyOverrideDialog() {
        String[] options = {"Disable for 1 Hour", "Disable for 2 Hours", "Disable for 4 Hours", "Cancel Override (Resume Automation)"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Manual Emergency Override")
                .setItems(options, (dialog, which) -> {
                    long duration = 0;
                    if (which == 0) duration = 3600000;
                    else if (which == 1) duration = 7200000;
                    else if (which == 2) duration = 14400000;

                    final long durationMs = duration;
                    final long overrideTime = durationMs > 0 ? (System.currentTimeMillis() + durationMs) : 0;

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        SettingsEntity settings = database.settingsDao().getSettingsSync();
                        if (settings != null) {
                            settings.setOverrideUntilTimestamp(overrideTime);
                            database.settingsDao().insertOrUpdate(settings);
                        }
                        contextEngine.evaluateAndApplyContext(durationMs > 0 ? "MANUAL_OVERRIDE" : "RESUME_AUTOMATION");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    if (overrideTime > 0) {
                                        Toast.makeText(requireContext(), "Manual Override activated!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "Manual Override cancelled. Automation active.", Toast.LENGTH_SHORT).show();
                                    }
                                    updateDashboardData();
                                }
                            });
                        }
                    });
                })
                .show();
    }
}

