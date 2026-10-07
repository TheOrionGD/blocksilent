package com.blocksilent.app.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
    private ContactAdapter adapter;
    private AppDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_important_contacts, container, false);

        rvContacts = view.findViewById(R.id.rvImportantContacts);
        btnAddContact = view.findViewById(R.id.btnAddContact);
        btnDndSettings = view.findViewById(R.id.btnOpenDndPrioritySettings);

        rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactAdapter(requireContext(), this);
        rvContacts.setAdapter(adapter);

        database = AppDatabase.getInstance(requireContext().getApplicationContext());
        database.contactDao().getAllContacts().observe(getViewLifecycleOwner(), contacts -> adapter.setContacts(contacts));

        btnAddContact.setOnClickListener(v -> showAddContactDialog());
        btnDndSettings.setOnClickListener(v -> PermissionManager.openDndSettings(requireContext()));

        return view;
    }

    private void showAddContactDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_contact, null);
        EditText etName = view.findViewById(R.id.etContactName);
        EditText etPhone = view.findViewById(R.id.etContactPhone);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Important Contact")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();

                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(requireContext(), "Contact name is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.contactDao().insert(new ContactEntity(name, phone, true));
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDelete(ContactEntity contact) {
        AppDatabase.databaseWriteExecutor.execute(() -> database.contactDao().delete(contact));
    }
}
