package com.blocksilent.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.database.entities.ContactEntity;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    public interface OnContactActionListener {
        void onDelete(ContactEntity contact);
    }

    private final Context context;
    private List<ContactEntity> contactList = new ArrayList<>();
    private final OnContactActionListener listener;

    public ContactAdapter(Context context, OnContactActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setContacts(List<ContactEntity> contacts) {
        this.contactList = (contacts != null) ? contacts : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactEntity item = contactList.get(position);
        holder.tvContactName.setText(item.getName());
        holder.tvPhone.setText(item.getPhoneNumber());
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvContactName, tvPhone;
        Button btnDelete;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContactName = itemView.findViewById(R.id.tvContactName);
            tvPhone = itemView.findViewById(R.id.tvContactPhone);
            btnDelete = itemView.findViewById(R.id.btnDeleteContact);
        }
    }
}
