package com.blocksilent.app.fragments;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.adapters.TimetableAdapter;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.database.entities.TimetableEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TimetableFragment extends Fragment implements TimetableAdapter.OnTimetableActionListener {

    private RecyclerView rvTimetable;
    private Button btnAddTimetable;
    private TimetableAdapter adapter;
    private AppDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timetable, container, false);

        rvTimetable = view.findViewById(R.id.rvTimetable);
        btnAddTimetable = view.findViewById(R.id.btnAddTimetable);

        rvTimetable.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TimetableAdapter(requireContext(), this);
        rvTimetable.setAdapter(adapter);

        database = AppDatabase.getInstance(requireContext().getApplicationContext());
        database.timetableDao().getAllTimetables().observe(getViewLifecycleOwner(), timetables -> adapter.setTimetables(timetables));

        btnAddTimetable.setOnClickListener(v -> showAddTimetableDialog());

        return view;
    }

    private void showAddTimetableDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_timetable, null);
        EditText etSubject = dialogView.findViewById(R.id.etDialogSubject);
        Spinner spinnerDay = dialogView.findViewById(R.id.spinnerDialogDay);
        Button btnStartTime = dialogView.findViewById(R.id.btnDialogStartTime);
        Button btnEndTime = dialogView.findViewById(R.id.btnDialogEndTime);
        Spinner spinnerBlock = dialogView.findViewById(R.id.spinnerDialogBlock);
        Spinner spinnerSoundMode = dialogView.findViewById(R.id.spinnerDialogSoundMode);

        final String[] startTimeHolder = {"09:00 AM"};
        final String[] endTimeHolder = {"10:00 AM"};

        btnStartTime.setOnClickListener(v -> {
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker = new TimePickerDialog(requireContext(), (timePicker, selectedHour, selectedMinute) -> {
                String amPm = selectedHour >= 12 ? "PM" : "AM";
                int h = selectedHour % 12;
                if (h == 0) h = 12;
                startTimeHolder[0] = String.format(Locale.getDefault(), "%02d:%02d %s", h, selectedMinute, amPm);
                btnStartTime.setText(startTimeHolder[0]);
            }, hour, minute, false);
            mTimePicker.setTitle("Select Start Time");
            mTimePicker.show();
        });

        btnEndTime.setOnClickListener(v -> {
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker = new TimePickerDialog(requireContext(), (timePicker, selectedHour, selectedMinute) -> {
                String amPm = selectedHour >= 12 ? "PM" : "AM";
                int h = selectedHour % 12;
                if (h == 0) h = 12;
                endTimeHolder[0] = String.format(Locale.getDefault(), "%02d:%02d %s", h, selectedMinute, amPm);
                btnEndTime.setText(endTimeHolder[0]);
            }, hour, minute, false);
            mTimePicker.setTitle("Select End Time");
            mTimePicker.show();
        });

        // Populate days
        String[] days = new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, days);
        spinnerDay.setAdapter(dayAdapter);

        // Populate sound modes
        String[] modes = new String[]{"SILENT", "VIBRATE", "NORMAL"};
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, modes);
        spinnerSoundMode.setAdapter(modeAdapter);

        // Populate Blocks from DB
        List<BlockEntity> blockList = new ArrayList<>();
        List<String> blockNames = new ArrayList<>();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<BlockEntity> blocks = database.blockDao().getAllBlocksSync();
            if (blocks != null) {
                blockList.addAll(blocks);
                for (BlockEntity b : blocks) {
                    blockNames.add(b.getName());
                }
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> blockAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, blockNames);
                    spinnerBlock.setAdapter(blockAdapter);
                });
            }
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Class Timetable")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String subject = etSubject.getText().toString().trim();
                    if (TextUtils.isEmpty(subject)) {
                        Toast.makeText(requireContext(), "Subject name required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String day = (String) spinnerDay.getSelectedItem();
                    String mode = (String) spinnerSoundMode.getSelectedItem();

                    int selectedBlockIndex = spinnerBlock.getSelectedItemPosition();
                    long blockId = (selectedBlockIndex >= 0 && selectedBlockIndex < blockList.size()) ? blockList.get(selectedBlockIndex).getId() : 1;
                    String blockName = (selectedBlockIndex >= 0 && selectedBlockIndex < blockList.size()) ? blockList.get(selectedBlockIndex).getName() : "CSE Block";

                    TimetableEntity timetable = new TimetableEntity(
                            subject, day, startTimeHolder[0], endTimeHolder[0], blockId, blockName, mode, true
                    );

                    AppDatabase.databaseWriteExecutor.execute(() -> database.timetableDao().insert(timetable));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDelete(TimetableEntity timetable) {
        AppDatabase.databaseWriteExecutor.execute(() -> database.timetableDao().delete(timetable));
    }

    @Override
    public void onToggleEnable(TimetableEntity timetable, boolean enabled) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            timetable.setEnabled(enabled);
            database.timetableDao().update(timetable);
        });
    }
}
