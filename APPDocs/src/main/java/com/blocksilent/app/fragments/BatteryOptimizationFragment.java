package com.blocksilent.app.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blocksilent.app.R;

public class BatteryOptimizationFragment extends Fragment {

    private Button btnBatterySettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_battery_optimization, container, false);

        btnBatterySettings = view.findViewById(R.id.btnDisableBatteryOptimization);
        btnBatterySettings.setOnClickListener(v -> openBatteryOptimizationSettings());

        return view;
    }

    private void openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Intent appSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                appSettings.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(appSettings);
            }
        }
    }
}
