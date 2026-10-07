package com.blocksilent.app.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.adapters.ContactAdapter;
import com.blocksilent.app.database.AppDatabase;
import com.blocksilent.app.database.entities.ContactEntity;
import com.blocksilent.app.utils.PermissionManager;

public class ImportantContactsFragment extends Fragment implements ContactAdapter.OnContactActionListener {

    private RecyclerView rvContacts;
    private Button btnAddContact, btnDndSettings;
    private LinearLayout layoutEmptyContacts;
    private ContactAdapter adapter;
    private AppDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_important_contacts, container, false);

        rvContacts = view.findViewById(R.id.rvImportantContacts);
        btnAddContact = view.findViewById(R.id.btnAddContact);
        btnDndSettings = view.findViewById(R.id.btnOpenDndPrioritySettings);
        layoutEmptyContacts = view.findViewById(R.id.layoutEmptyContacts);

        rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactAdapter(requireContext(), this);
        rvContacts.setAdapter(adapter);

        Context ctx = getContext();
        if (ctx != null) {
            database = AppDatabase.getInstance(ctx.getApplicationContext());
            database.contactDao().getAllContacts().observe(getViewLifecycleOwner(), contacts -> {
                if (isAdded()) {
                    adapter.setContacts(contacts);
                    if (contacts == null || contacts.isEmpty()) {
                        layoutEmptyContacts.setVisibility(View.VISIBLE);
                        rvContacts.setVisibility(View.GONE);
                    } else {
                        layoutEmptyContacts.setVisibility(View.GONE);
                        rvContacts.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        btnAddContact.setOnClickListener(v -> showAddContactDialog());
        btnDndSettings.setOnClickListener(v -> {
            if (isAdded() && getContext() != null) {
                PermissionManager.openDndSettings(requireContext());
            }
        });

        return view;
    }

    private void showAddContactDialog() {
        if (!isAdded() || getContext() == null) return;

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_contact, null);
        EditText etName = view.findViewById(R.id.etContactName);
        EditText etPhone = view.findViewById(R.id.etContactPhone);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add VIP Contact")
                .setView(view)
                .setPositiveButton("Save Contact", (dialog, which) -> {
                    if (!isAdded() || getContext() == null) return;
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(requireContext(), "Contact name is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        if (database != null) {
                            database.contactDao().insert(new ContactEntity(name, phone, true));
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDelete(ContactEntity contact) {
        if (contact == null) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (database != null) {
                database.contactDao().delete(contact);
            }
        });
    }
}
