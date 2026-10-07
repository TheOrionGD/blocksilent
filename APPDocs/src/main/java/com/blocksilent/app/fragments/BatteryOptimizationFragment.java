package com.blocksilent.app.fragments;
 
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blocksilent.app.R;
import com.blocksilent.app.utils.BatteryOptimizationHelper;

public class BatteryOptimizationFragment extends Fragment {

    private TextView tvBatteryStatusBadge;
    private TextView tvBatteryStatusExplanation;
    private Button btnBatterySettings;
    private Button btnOpenAppSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_battery_optimization, container, false);

        tvBatteryStatusBadge = view.findViewById(R.id.tvBatteryStatusBadge);
        tvBatteryStatusExplanation = view.findViewById(R.id.tvBatteryStatusExplanation);
        btnBatterySettings = view.findViewById(R.id.btnDisableBatteryOptimization);
        btnOpenAppSettings = view.findViewById(R.id.btnOpenAppSettings);

        btnBatterySettings.setOnClickListener(v -> {
            if (isAdded() && getContext() != null) {
                BatteryOptimizationHelper.requestIgnoreBatteryOptimization(requireContext());
            }
        });

        btnOpenAppSettings.setOnClickListener(v -> {
            if (isAdded() && getContext() != null) {
                BatteryOptimizationHelper.openAppSettings(requireContext());
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        if (!isAdded() || getContext() == null) return;

        boolean isIgnored = BatteryOptimizationHelper.isBatteryOptimizationIgnored(requireContext());

        if (isIgnored) {
            tvBatteryStatusBadge.setText("● UNRESTRICTED");
            tvBatteryStatusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_active));
            tvBatteryStatusBadge.setBackgroundResource(R.drawable.bg_badge_active);
            tvBatteryStatusExplanation.setText("✓ Battery optimization is disabled for BlockSilent. Background geofencing and automation will run reliably even with the screen off.");
            btnBatterySettings.setText("Battery Settings Configured");
            btnBatterySettings.setEnabled(false);
        } else {
            tvBatteryStatusBadge.setText("● RESTRICTED");
            tvBatteryStatusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_inactive));
            tvBatteryStatusBadge.setBackgroundResource(R.drawable.bg_badge_inactive);
            tvBatteryStatusExplanation.setText("Battery optimization is currently active. The Android system may sleep or delay geofence triggers when your screen is turned off.");
            btnBatterySettings.setText("Request Unrestricted Battery Access");
            btnBatterySettings.setEnabled(true);
        }
    }
}

