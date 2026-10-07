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
import androidx.fragment.app.Fragment;

import com.blocksilent.app.R;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.WearableSettingsEntity;
import com.blocksilent.app.wearable.WearableNotificationHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class WearableSettingsFragment extends Fragment {

    private SwitchMaterial switchWearableSync;
    private Button btnTestWearableHaptic;

    private AppDatabase database;
    private WearableNotificationHelper wearableHelper;
    private WearableSettingsEntity currentSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wearable_settings, container, false);

        Context ctx = requireContext();
        database = AppDatabase.getInstance(ctx.getApplicationContext());
        wearableHelper = new WearableNotificationHelper(ctx);

        switchWearableSync = view.findViewById(R.id.switchWearableSync);
        btnTestWearableHaptic = view.findViewById(R.id.btnTestWearableHaptic);

        view.findViewById(R.id.btnWearableBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        loadSettingsFromDb();

        btnTestWearableHaptic.setOnClickListener(v -> {
            wearableHelper.notifyZoneTransition(
                    "Wearable Haptic Test",
                    "Synchronized vibration pulse dispatched to smartwatch.",
                    "ENTER"
            );
            Toast.makeText(requireContext(), "Dispatched haptic pulse to wearable.", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadSettingsFromDb() {
        database.wearableSettingsDao().getWearableSettings().observe(getViewLifecycleOwner(), settings -> {
            if (settings != null) {
                currentSettings = settings;
                switchWearableSync.setOnCheckedChangeListener(null);
                switchWearableSync.setChecked(settings.isWearableSyncEnabled());

                switchWearableSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    settings.setWearableSyncEnabled(isChecked);
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.wearableSettingsDao().insertOrUpdate(settings);
                    });
                });
            }
        });
    }
}
