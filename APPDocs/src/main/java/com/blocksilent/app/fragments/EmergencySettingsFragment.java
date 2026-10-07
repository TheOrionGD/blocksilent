package com.blocksilent.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.adapters.EmergencyContactAdapter;
import com.blocksilent.app.context.ContextEngine;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.EmergencyContactEntity;
import com.blocksilent.app.database.entities.SettingsEntity;
import com.blocksilent.app.emergency.EmergencyManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmergencySettingsFragment extends Fragment implements EmergencyContactAdapter.OnEmergencyActionListener, EmergencyManager.EmergencyStateListener {

    private MaterialCardView cardEmergencyActiveStatus;
    private TextView tvEmergencyStatusBanner, tvManualOverrideStatus;
    private Button btnAcknowledgeEmergency, btnPause1Hour, btnPause2Hours, btnResumeAutomation, btnAddEmergContact;
    private RecyclerView rvEmergContacts;

    private AppDatabase database;
    private EmergencyManager emergencyManager;
    private ContextEngine contextEngine;
    private EmergencyContactAdapter contactAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_emergency_settings, container, false);

        Context ctx = requireContext();
        database = AppDatabase.getInstance(ctx.getApplicationContext());
        emergencyManager = new EmergencyManager(ctx);
        contextEngine = ContextEngine.getInstance(ctx);
        emergencyManager.setListener(this);

        cardEmergencyActiveStatus = view.findViewById(R.id.cardEmergencyActiveStatus);
        tvEmergencyStatusBanner = view.findViewById(R.id.tvEmergencyStatusBanner);
        tvManualOverrideStatus = view.findViewById(R.id.tvManualOverrideStatus);

        view.findViewById(R.id.btnEmergBack).setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        btnAcknowledgeEmergency = view.findViewById(R.id.btnAcknowledgeEmergency);
        btnPause1Hour = view.findViewById(R.id.btnPause1Hour);
        btnPause2Hours = view.findViewById(R.id.btnPause2Hours);
        btnResumeAutomation = view.findViewById(R.id.btnResumeAutomation);
        btnAddEmergContact = view.findViewById(R.id.btnAddEmergContact);
        rvEmergContacts = view.findViewById(R.id.rvEmergContacts);

        setupRecyclerView();
        setupListeners();
        observeContacts();
        observeSettings();

        return view;
    }

    private void setupRecyclerView() {
        rvEmergContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        contactAdapter = new EmergencyContactAdapter(requireContext(), this);
        rvEmergContacts.setAdapter(contactAdapter);
    }

    private void observeContacts() {
        database.emergencyContactDao().getAllContacts().observe(getViewLifecycleOwner(), contacts -> {
            if (isAdded()) {
                contactAdapter.setContacts(contacts);
            }
        });
    }

    private void observeSettings() {
        database.settingsDao().getSettings().observe(getViewLifecycleOwner(), settings -> {
            if (!isAdded() || settings == null) return;
            long now = System.currentTimeMillis();
            if (settings.getOverrideUntilTimestamp() > now) {
                String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(settings.getOverrideUntilTimestamp()));
                tvManualOverrideStatus.setText("Override Status: Active until " + timeStr);
                tvManualOverrideStatus.setTextColor(0xFFFF5722);
            } else {
                tvManualOverrideStatus.setText("Override Status: Not active (Automation running)");
                tvManualOverrideStatus.setTextColor(0xFF4CAF50);
            }
        });
    }

    private void setupListeners() {
        btnAcknowledgeEmergency.setOnClickListener(v -> {
            emergencyManager.acknowledgeAndStopEmergency();
            cardEmergencyActiveStatus.setVisibility(View.GONE);
            contextEngine.updateEmergencyState(false);
            Toast.makeText(requireContext(), "Emergency siren stopped and acknowledged.", Toast.LENGTH_SHORT).show();
        });

        btnPause1Hour.setOnClickListener(v -> setManualOverride(1 * 60 * 60 * 1000L));
        btnPause2Hours.setOnClickListener(v -> setManualOverride(2 * 60 * 60 * 1000L));
        btnResumeAutomation.setOnClickListener(v -> setManualOverride(0));

        btnAddEmergContact.setOnClickListener(v -> showAddContactDialog());
    }

    private void setManualOverride(long durationMs) {
        long until = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            SettingsEntity settings = database.settingsDao().getSettingsSync();
            if (settings != null) {
                settings.setOverrideUntilTimestamp(until);
                database.settingsDao().insertOrUpdate(settings);
            }
            contextEngine.evaluateAndApplyContext(durationMs > 0 ? "MANUAL_OVERRIDE" : "RESUME_AUTOMATION");
        });
    }

    private void showAddContactDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_emergency_contact, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etEmergContactName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etEmergContactPhone);
        Spinner spinnerPriority = dialogView.findViewById(R.id.spinnerEmergPriority);
        CheckBox cbKeyword = dialogView.findViewById(R.id.cbDialogKeyword);
        CheckBox cbSms = dialogView.findViewById(R.id.cbDialogSms);

        String[] priorities = new String[]{"Priority 1 (Highest - Siren & Override)", "Priority 2 (High - Override)", "Priority 3 (Normal)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, priorities);
        spinnerPriority.setAdapter(adapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add VIP Emergency Contact")
                .setView(dialogView)
                .setPositiveButton("Add Contact", (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                        Toast.makeText(requireContext(), "Name and phone number are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int priority = spinnerPriority.getSelectedItemPosition() + 1;
                    boolean keyword = cbKeyword.isChecked();
                    boolean sms = cbSms.isChecked();

                    final EmergencyContactEntity newContact = new EmergencyContactEntity(
                            name, phone, priority, sms, keyword, 0L, System.currentTimeMillis()
                    );

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.emergencyContactDao().insertOrUpdate(newContact);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onUpdateContact(EmergencyContactEntity contact) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.emergencyContactDao().update(contact);
        });
    }

    @Override
    public void onDeleteContact(EmergencyContactEntity contact) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.emergencyContactDao().delete(contact);
        });
    }

    @Override
    public void onEmergencyTriggered(String contactName, String source) {
        if (!isAdded()) return;
        android.app.Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (isAdded()) {
                    cardEmergencyActiveStatus.setVisibility(View.VISIBLE);
                    tvEmergencyStatusBanner.setText("🚨 EMERGENCY OVERRIDE ACTIVE FROM " + (contactName != null ? contactName.toUpperCase() : "VIP CONTACT"));
                }
            });
        }
        contextEngine.updateEmergencyState(true);
    }

    @Override
    public void onEmergencyAcknowledged() {
        if (!isAdded()) return;
        android.app.Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
                if (isAdded()) {
                    cardEmergencyActiveStatus.setVisibility(View.GONE);
                }
            });
        }
        contextEngine.updateEmergencyState(false);
    }
}
