package com.blocksilent.app.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.adapters.NoiseSampleAdapter;
import com.blocksilent.app.context.ContextEngine;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.noise.NoiseDetector;

public class NoiseDetectionFragment extends Fragment {

    private TextView tvLiveNoiseDb, tvLiveNoiseClassification;
    private Button btnMeasureNoiseOnce;
    private RecyclerView rvNoiseHistory;

    private NoiseDetector noiseDetector;
    private ContextEngine contextEngine;
    private AppDatabase database;
    private NoiseSampleAdapter adapter;

    private ActivityResultLauncher<String> requestMicPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestMicPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        sampleCurrentNoise();
                    } else {
                        Toast.makeText(requireContext(), "Microphone permission is required to sample noise levels.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_noise_detection, container, false);

        Context ctx = requireContext();
        noiseDetector = new NoiseDetector(ctx);
        contextEngine = ContextEngine.getInstance(ctx);
        database = AppDatabase.getInstance(ctx.getApplicationContext());

        tvLiveNoiseDb = view.findViewById(R.id.tvLiveNoiseDb);
        tvLiveNoiseClassification = view.findViewById(R.id.tvLiveNoiseClassification);
        btnMeasureNoiseOnce = view.findViewById(R.id.btnMeasureNoiseOnce);
        rvNoiseHistory = view.findViewById(R.id.rvNoiseHistory);

        view.findViewById(R.id.btnNoiseBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        rvNoiseHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NoiseSampleAdapter(requireContext());
        rvNoiseHistory.setAdapter(adapter);

        btnMeasureNoiseOnce.setOnClickListener(v -> checkPermissionAndSample());

        observeHistory();

        return view;
    }

    private void observeHistory() {
        database.noiseSampleDao().getAllSamples().observe(getViewLifecycleOwner(), samples -> {
            if (isAdded()) {
                adapter.setSamples(samples);
            }
        });
    }

    private void checkPermissionAndSample() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            sampleCurrentNoise();
        } else {
            requestMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void sampleCurrentNoise() {
        btnMeasureNoiseOnce.setEnabled(false);
        btnMeasureNoiseOnce.setText("Sampling...");

        noiseDetector.sampleAmbientNoiseOnce(0, "Current Campus Zone", (dB, category) -> {
            if (!isAdded()) return;
            btnMeasureNoiseOnce.setEnabled(true);
            btnMeasureNoiseOnce.setText("Sample Environment Noise");

            tvLiveNoiseDb.setText(String.format("%.1f dB", dB));
            tvLiveNoiseClassification.setText(category);

            if (dB < 40.0) {
                tvLiveNoiseDb.setTextColor(Color.parseColor("#4CAF50"));
            } else if (dB <= 60.0) {
                tvLiveNoiseDb.setTextColor(Color.parseColor("#FFC107"));
            } else {
                tvLiveNoiseDb.setTextColor(Color.parseColor("#F44336"));
            }

            contextEngine.updateNoiseSample(dB);
        });
    }
}
