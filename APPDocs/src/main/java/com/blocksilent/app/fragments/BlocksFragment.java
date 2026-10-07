package com.blocksilent.app.fragments;

import android.content.Intent;
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
import com.blocksilent.app.activities.AddEditBlockActivity;
import com.blocksilent.app.adapters.BlockAdapter;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.BlockEntity;
import com.blocksilent.app.geofence.GeofenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class BlocksFragment extends Fragment implements BlockAdapter.OnBlockActionListener {

    private RecyclerView rvBlocks;
    private FloatingActionButton fabAddBlock;
    private TextView tvBlocksSubtitle;
    private LinearLayout layoutEmptyBlocks;
    private Button btnEmptyAddBlock;

    private BlockAdapter adapter;
    private AppDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blocks, container, false);

        rvBlocks = view.findViewById(R.id.rvBlocks);
        fabAddBlock = view.findViewById(R.id.fabAddBlock);
        tvBlocksSubtitle = view.findViewById(R.id.tvBlocksSubtitle);
        layoutEmptyBlocks = view.findViewById(R.id.layoutEmptyBlocks);
        btnEmptyAddBlock = view.findViewById(R.id.btnEmptyAddBlock);

        rvBlocks.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BlockAdapter(requireContext(), this);
        rvBlocks.setAdapter(adapter);

        database = AppDatabase.getInstance(requireContext().getApplicationContext());
        database.blockDao().getAllBlocks().observe(getViewLifecycleOwner(), blocks -> {
            if (isAdded()) {
                adapter.setBlocks(blocks);
                int count = (blocks != null) ? blocks.size() : 0;
                tvBlocksSubtitle.setText(count + " geofenced campus zones configured");

                if (count == 0) {
                    layoutEmptyBlocks.setVisibility(View.VISIBLE);
                    rvBlocks.setVisibility(View.GONE);
                } else {
                    layoutEmptyBlocks.setVisibility(View.GONE);
                    rvBlocks.setVisibility(View.VISIBLE);
                }
            }
        });

        fabAddBlock.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddEditBlockActivity.class)));
        btnEmptyAddBlock.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddEditBlockActivity.class)));

        return view;
    }

    @Override
    public void onEdit(BlockEntity block) {
        Intent intent = new Intent(requireContext(), AddEditBlockActivity.class);
        intent.putExtra(AddEditBlockActivity.EXTRA_BLOCK_ID, block.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(BlockEntity block) {
        if (!isAdded() || getContext() == null) return;
        android.content.Context appContext = requireContext().getApplicationContext();
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Block")
                .setMessage("Are you sure you want to delete " + block.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.blockDao().delete(block);

                        // Re-register remaining geofences
                        List<BlockEntity> enabledBlocks = database.blockDao().getEnabledBlocksSync();
                        GeofenceManager geofenceManager = new GeofenceManager(appContext);
                        geofenceManager.registerGeofences(enabledBlocks);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded() && getContext() != null) {
                                    Toast.makeText(requireContext(), "Block deleted.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onToggleEnable(BlockEntity block, boolean enabled) {
        if (getContext() == null) return;
        android.content.Context appContext = requireContext().getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            block.setEnabled(enabled);
            database.blockDao().update(block);

            List<BlockEntity> enabledBlocks = database.blockDao().getEnabledBlocksSync();
            GeofenceManager geofenceManager = new GeofenceManager(appContext);
            geofenceManager.registerGeofences(enabledBlocks);
        });
    }
}
