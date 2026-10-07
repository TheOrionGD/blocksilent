package com.blocksilent.app.fragments;

import android.app.TimePickerDialog;
import android.content.Context;
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

        Context ctx = getContext();
        if (ctx != null) {
            database = AppDatabase.getInstance(ctx.getApplicationContext());
            database.timetableDao().getAllTimetables().observe(getViewLifecycleOwner(), timetables -> {
                if (isAdded()) {
                    adapter.setTimetables(timetables);
                }
            });
        }

        btnAddTimetable.setOnClickListener(v -> showAddTimetableDialog());

        return view;
    }

    private void showAddTimetableDialog() {
        if (!isAdded() || getContext() == null) return;

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
            if (!isAdded() || getContext() == null) return;
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker = new TimePickerDialog(requireContext(), (timePicker, selectedHour, selectedMinute) -> {
                String amPm = selectedHour >= 12 ? "PM" : "AM";
                int h = selectedHour % 12;
                if (h == 0) h = 12;
                startTimeHolder[0] = String.format(Locale.US, "%02d:%02d %s", h, selectedMinute, amPm);
                btnStartTime.setText(startTimeHolder[0]);
            }, hour, minute, false);
            mTimePicker.setTitle("Select Start Time");
            mTimePicker.show();
        });

        btnEndTime.setOnClickListener(v -> {
            if (!isAdded() || getContext() == null) return;
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker = new TimePickerDialog(requireContext(), (timePicker, selectedHour, selectedMinute) -> {
                String amPm = selectedHour >= 12 ? "PM" : "AM";
                int h = selectedHour % 12;
                if (h == 0) h = 12;
                endTimeHolder[0] = String.format(Locale.US, "%02d:%02d %s", h, selectedMinute, amPm);
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
            if (database == null) return;
            List<BlockEntity> blocks = database.blockDao().getAllBlocksSync();
            if (blocks != null) {
                blockList.addAll(blocks);
                for (BlockEntity b : blocks) {
                    blockNames.add(b.getName());
                }
            }
            if (blockNames.isEmpty()) {
                blockNames.add("Default Campus Area");
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        ArrayAdapter<String> blockAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, blockNames);
                        spinnerBlock.setAdapter(blockAdapter);
                    }
                });
            }
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Class Timetable")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    if (!isAdded() || getContext() == null) return;
                    String subject = etSubject.getText() != null ? etSubject.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(subject)) {
                        Toast.makeText(requireContext(), "Subject name required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String day = spinnerDay.getSelectedItem() != null ? (String) spinnerDay.getSelectedItem() : "Monday";
                    String mode = spinnerSoundMode.getSelectedItem() != null ? (String) spinnerSoundMode.getSelectedItem() : "SILENT";

                    int selectedBlockIndex = spinnerBlock.getSelectedItemPosition();
                    long blockId = (selectedBlockIndex >= 0 && selectedBlockIndex < blockList.size()) ? blockList.get(selectedBlockIndex).getId() : 1;
                    String blockName = (selectedBlockIndex >= 0 && selectedBlockIndex < blockList.size()) ? blockList.get(selectedBlockIndex).getName() : "Campus Block";

                    TimetableEntity timetable = new TimetableEntity(
                            subject, day, startTimeHolder[0], endTimeHolder[0], blockId, blockName, mode, true
                    );

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (database != null) {
                            database.timetableDao().insert(timetable);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDelete(TimetableEntity timetable) {
        if (!isAdded() || getContext() == null || timetable == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Timetable Entry")
                .setMessage("Are you sure you want to delete " + timetable.getSubjectName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (database != null) {
                            database.timetableDao().delete(timetable);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onToggleEnable(TimetableEntity timetable, boolean enabled) {
        if (timetable == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (database != null) {
                timetable.setEnabled(enabled);
                database.timetableDao().update(timetable);
            }
        });
    }
}
