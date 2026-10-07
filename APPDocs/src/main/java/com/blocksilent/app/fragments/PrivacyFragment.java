package com.blocksilent.app.fragments;

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
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.geofence.GeofenceManager;

public class PrivacyFragment extends Fragment {

    private Button btnClearDataPrivacy;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_privacy, container, false);

        btnClearDataPrivacy = view.findViewById(R.id.btnClearDataPrivacy);
        btnClearDataPrivacy.setOnClickListener(v -> showClearAllDataConfirmation());

        return view;
    }

    private void showClearAllDataConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete All Local Data?")
                .setMessage("This will permanently delete all saved blocks, timetables, history, and settings from your device. Are you sure?")
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AppDatabase database = AppDatabase.getInstance(requireContext().getApplicationContext());
                        GeofenceManager geofenceManager = new GeofenceManager(requireContext().getApplicationContext());
                        geofenceManager.unregisterAllGeofences();

                        database.blockDao().deleteAll();
                        database.timetableDao().deleteAll();
                        database.historyDao().deleteAll();
                        database.contactDao().deleteAll();
                        database.settingsDao().deleteAll();

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "All local data deleted.", Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
