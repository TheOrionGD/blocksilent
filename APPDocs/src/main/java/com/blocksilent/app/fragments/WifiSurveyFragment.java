package com.blocksilent.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.adapters.WifiZoneAdapter;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.WifiZoneEntity;
import com.blocksilent.app.wifi.WifiSurveyManager;
import com.blocksilent.app.wifi.WifiZoneManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class WifiSurveyFragment extends Fragment implements WifiZoneAdapter.OnWifiZoneActionListener {

    private TextView tvSurveySsid, tvSurveyBssid, tvSurveyRssi, tvSavedZonesCount;
    private Button btnRefreshSurvey, btnSaveWifiZone;
    private TextInputEditText etSurveyRoomName, etSurveyFloor;
    private Spinner spinnerSurveyBlocks, spinnerSurveyPolicy;
    private RecyclerView rvSurveySavedZones;

    private WifiSurveyManager surveyManager;
    private AppDatabase database;
    private WifiZoneAdapter zoneAdapter;

    private final List<BlockEntity> availableBlocks = new ArrayList<>();
    private final List<String> blockNames = new ArrayList<>();
    private ArrayAdapter<String> blockSpinnerAdapter;

    private WifiZoneManager.WifiConnectionInfo currentSurveyInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_survey, container, false);

        Context ctx = requireContext();
        surveyManager = new WifiSurveyManager(ctx);
        database = AppDatabase.getInstance(ctx.getApplicationContext());

        tvSurveySsid = view.findViewById(R.id.tvSurveySsid);
        tvSurveyBssid = view.findViewById(R.id.tvSurveyBssid);
        tvSurveyRssi = view.findViewById(R.id.tvSurveyRssi);
        tvSavedZonesCount = view.findViewById(R.id.tvSavedZonesCount);

        view.findViewById(R.id.btnWifiSurveyBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnRefreshSurvey = view.findViewById(R.id.btnRefreshSurvey);
        btnSaveWifiZone = view.findViewById(R.id.btnSaveWifiZone);

        etSurveyRoomName = view.findViewById(R.id.etSurveyRoomName);
        etSurveyFloor = view.findViewById(R.id.etSurveyFloor);

        spinnerSurveyBlocks = view.findViewById(R.id.spinnerSurveyBlocks);
        spinnerSurveyPolicy = view.findViewById(R.id.spinnerSurveyPolicy);
        rvSurveySavedZones = view.findViewById(R.id.rvSurveySavedZones);

        setupSpinners();
        setupRecyclerView();
        setupListeners();
        refreshCurrentWifiSurvey();
        observeSavedZones();

        return view;
    }

    private void setupSpinners() {
        blockSpinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, blockNames);
        spinnerSurveyBlocks.setAdapter(blockSpinnerAdapter);

        database.blockDao().getAllBlocks().observe(getViewLifecycleOwner(), blocks -> {
            availableBlocks.clear();
            blockNames.clear();
            if (blocks != null) {
                availableBlocks.addAll(blocks);
                for (BlockEntity b : blocks) {
                    blockNames.add(b.getName());
                }
            }
            if (blockNames.isEmpty()) {
                blockNames.add("No Blocks Created");
            }
            blockSpinnerAdapter.notifyDataSetChanged();
        });

        String[] policies = new String[]{"SILENT", "VIBRATE", "NORMAL"};
        ArrayAdapter<String> policyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, policies);
        spinnerSurveyPolicy.setAdapter(policyAdapter);
    }

    private void setupRecyclerView() {
        rvSurveySavedZones.setLayoutManager(new LinearLayoutManager(requireContext()));
        zoneAdapter = new WifiZoneAdapter(requireContext(), this);
        rvSurveySavedZones.setAdapter(zoneAdapter);
    }

    private void observeSavedZones() {
        database.wifiZoneDao().getAllWifiZones().observe(getViewLifecycleOwner(), zones -> {
            if (isAdded()) {
                zoneAdapter.setZones(zones);
                int count = zones != null ? zones.size() : 0;
                tvSavedZonesCount.setText(count + (count == 1 ? " AP" : " APs"));
            }
        });
    }

    private void refreshCurrentWifiSurvey() {
        currentSurveyInfo = surveyManager.getSurveyData();
        if (currentSurveyInfo != null && currentSurveyInfo.isConnected && !currentSurveyInfo.bssid.isEmpty()) {
            tvSurveySsid.setText("SSID: " + currentSurveyInfo.ssid);
            tvSurveyBssid.setText("BSSID: " + currentSurveyInfo.bssid);
            tvSurveyRssi.setText("Signal Strength: " + currentSurveyInfo.rssi + " dBm");
        } else {
            tvSurveySsid.setText("SSID: Disconnected / Permission Required");
            tvSurveyBssid.setText("BSSID: --:--:--:--:--:--");
            tvSurveyRssi.setText("Signal Strength: 0 dBm");
        }
    }

    private void setupListeners() {
        btnRefreshSurvey.setOnClickListener(v -> refreshCurrentWifiSurvey());

        btnSaveWifiZone.setOnClickListener(v -> {
            if (currentSurveyInfo == null || !currentSurveyInfo.isConnected || currentSurveyInfo.bssid.isEmpty()) {
                Toast.makeText(requireContext(), "Connect to Wi-Fi before saving an AP mapping.", Toast.LENGTH_SHORT).show();
                return;
            }

            String room = etSurveyRoomName.getText() != null ? etSurveyRoomName.getText().toString().trim() : "";
            String floor = etSurveyFloor.getText() != null ? etSurveyFloor.getText().toString().trim() : "1";

            if (TextUtils.isEmpty(room)) {
                Toast.makeText(requireContext(), "Please enter a Room/Lab name.", Toast.LENGTH_SHORT).show();
                return;
            }

            long selectedBlockId = -1;
            String selectedBlockName = "General Campus";
            int blockPos = spinnerSurveyBlocks.getSelectedItemPosition();
            if (blockPos >= 0 && blockPos < availableBlocks.size()) {
                selectedBlockId = availableBlocks.get(blockPos).getId();
                selectedBlockName = availableBlocks.get(blockPos).getName();
            }

            String soundPolicy = spinnerSurveyPolicy.getSelectedItem() != null ? spinnerSurveyPolicy.getSelectedItem().toString() : "SILENT";

            final WifiZoneEntity newZone = new WifiZoneEntity(
                    currentSurveyInfo.bssid,
                    currentSurveyInfo.ssid,
                    currentSurveyInfo.rssi > 0 ? -currentSurveyInfo.rssi : currentSurveyInfo.rssi,
                    room,
                    floor,
                    selectedBlockId,
                    selectedBlockName,
                    soundPolicy,
                    true,
                    System.currentTimeMillis()
            );

            AppDatabase.databaseWriteExecutor.execute(() -> {
                database.wifiZoneDao().insertOrUpdate(newZone);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(requireContext(), "Wi-Fi AP mapped to " + room + " successfully!", Toast.LENGTH_SHORT).show();
                            etSurveyRoomName.setText("");
                            etSurveyFloor.setText("");
                        }
                    });
                }
            });
        });
    }

    @Override
    public void onToggleZone(WifiZoneEntity zone, boolean isEnabled) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            zone.setEnabled(isEnabled);
            database.wifiZoneDao().update(zone);
        });
    }

    @Override
    public void onDeleteZone(WifiZoneEntity zone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.wifiZoneDao().delete(zone);
        });
    }
}
