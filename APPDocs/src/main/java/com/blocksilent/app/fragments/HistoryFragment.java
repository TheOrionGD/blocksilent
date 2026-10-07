package com.blocksilent.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.adapters.HistoryAdapter;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.AutomationDecisionEntity;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnHistoryItemClickListener {

    private RecyclerView rvHistory;
    private Button btnClearHistory;
    private TextView tvHistoryCount;
    private LinearLayout layoutEmptyHistory;
    private ChipGroup chipGroupFilter;

    private HistoryAdapter adapter;
    private AppDatabase database;
    private List<AutomationDecisionEntity> allDecisions = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount);
        layoutEmptyHistory = view.findViewById(R.id.layoutEmptyHistory);
        chipGroupFilter = view.findViewById(R.id.chipGroupHistoryFilter);

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(requireContext(), this);
        rvHistory.setAdapter(adapter);

        setupFilterChips();

        Context ctx = getContext();
        if (ctx != null) {
            database = AppDatabase.getInstance(ctx.getApplicationContext());
            database.automationDecisionDao().getAllDecisions().observe(getViewLifecycleOwner(), decisions -> {
                if (isAdded()) {
                    allDecisions = decisions != null ? decisions : new ArrayList<>();
                    applyFilter();
                }
            });
        }

        btnClearHistory.setOnClickListener(v -> showClearHistoryDialog());

        return view;
    }

    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipFilterAll) {
                currentFilter = "ALL";
            } else if (id == R.id.chipFilterGeo) {
                currentFilter = "GEO";
            } else if (id == R.id.chipFilterWifi) {
                currentFilter = "WIFI";
            } else if (id == R.id.chipFilterSensor) {
                currentFilter = "SENSOR";
            } else if (id == R.id.chipFilterEmergency) {
                currentFilter = "EMERGENCY";
            } else if (id == R.id.chipFilterTimetable) {
                currentFilter = "TIMETABLE";
            }
            applyFilter();
        });
    }

    private void applyFilter() {
        List<AutomationDecisionEntity> filtered = new ArrayList<>();
        for (AutomationDecisionEntity d : allDecisions) {
            if ("ALL".equals(currentFilter)) {
                filtered.add(d);
            } else if (d.getSource() != null && d.getSource().toUpperCase().contains(currentFilter)) {
                filtered.add(d);
            }
        }

        adapter.setDecisions(filtered);
        tvHistoryCount.setText(filtered.size() + " events matching filter (" + allDecisions.size() + " total)");

        if (filtered.isEmpty()) {
            layoutEmptyHistory.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            layoutEmptyHistory.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AutomationDecisionEntity item) {
        if (!isAdded() || getContext() == null || item == null) return;

        String details = "Timestamp: " + item.getFormattedTime() + "\n\n"
                + "Context Source: " + item.getSource() + "\n"
                + "Location / Target: " + item.getLocation() + "\n"
                + "Applied Mode: " + item.getNewMode() + " (Previous: " + item.getPreviousMode() + ")\n"
                + "Rule Priority: " + item.getPriority() + " / 100\n"
                + "Confidence: " + item.getConfidence() + "\n\n"
                + "Explanation / Trigger Reason:\n" + item.getReason();

        new AlertDialog.Builder(requireContext())
                .setTitle("Context Decision Details")
                .setMessage(details)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showClearHistoryDialog() {
        if (!isAdded() || getContext() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear History Logs")
                .setMessage("Are you sure you want to clear all automation history and decision logs?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (database != null) {
                            database.automationDecisionDao().deleteAll();
                            database.historyDao().deleteAll();
                        }
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "History logs cleared.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
