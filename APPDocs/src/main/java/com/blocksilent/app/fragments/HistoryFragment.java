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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.adapters.HistoryAdapter;
import com.blocksilent.app.database.AppDatabase;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private Button btnClearHistory;
    private HistoryAdapter adapter;
    private AppDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory = view.findViewById(R.id.rvHistory);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(requireContext());
        rvHistory.setAdapter(adapter);

        Context ctx = getContext();
        if (ctx != null) {
            database = AppDatabase.getInstance(ctx.getApplicationContext());
            database.historyDao().getAllHistory().observe(getViewLifecycleOwner(), history -> {
                if (isAdded()) {
                    adapter.setHistory(history);
                }
            });
        }

        btnClearHistory.setOnClickListener(v -> {
            if (!isAdded() || getContext() == null) return;
            new AlertDialog.Builder(requireContext())
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to clear all activity history logs?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            if (database != null) {
                                database.historyDao().deleteAll();
                            }
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (isAdded() && getContext() != null) {
                                        Toast.makeText(requireContext(), "History cleared.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return view;
    }
}
