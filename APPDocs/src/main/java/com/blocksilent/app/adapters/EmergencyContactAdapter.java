package com.blocksilent.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.database.entities.EmergencyContactEntity;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    public interface OnEmergencyActionListener {
        void onUpdateContact(EmergencyContactEntity contact);
        void onDeleteContact(EmergencyContactEntity contact);
    }

    private final Context context;
    private final OnEmergencyActionListener listener;
    private List<EmergencyContactEntity> contacts = new ArrayList<>();

    public EmergencyContactAdapter(Context context, OnEmergencyActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setContacts(List<EmergencyContactEntity> contactList) {
        this.contacts = contactList != null ? contactList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_emergency_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContactEntity contact = contacts.get(position);
        holder.tvName.setText(contact.getName());
        holder.tvPhone.setText(contact.getPhoneNumber());
        holder.tvBadge.setText("Priority " + contact.getPriority());

        holder.cbKeyword.setOnCheckedChangeListener(null);
        holder.cbSms.setOnCheckedChangeListener(null);

        holder.cbKeyword.setChecked(contact.isKeywordEnabled());
        holder.cbSms.setChecked(contact.isSmsEnabled());

        holder.cbKeyword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            contact.setKeywordEnabled(isChecked);
            if (listener != null) listener.onUpdateContact(contact);
        });

        holder.cbSms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            contact.setSmsEnabled(isChecked);
            if (listener != null) listener.onUpdateContact(contact);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteContact(contact);
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvBadge;
        CheckBox cbKeyword, cbSms;
        ImageButton btnDelete;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmergencyContactName);
            tvPhone = itemView.findViewById(R.id.tvEmergencyContactPhone);
            tvBadge = itemView.findViewById(R.id.tvEmergencyPriorityBadge);
            cbKeyword = itemView.findViewById(R.id.cbKeywordEnabled);
            cbSms = itemView.findViewById(R.id.cbSmsEnabled);
            btnDelete = itemView.findViewById(R.id.btnDeleteEmergencyContact);
        }
    }
}
