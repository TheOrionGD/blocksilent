package com.blocksilent.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.database.entities.HistoryEntity;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private List<HistoryEntity> historyList = new ArrayList<>();

    public HistoryAdapter(Context context) {
        this.context = context;
    }

    public void setHistory(List<HistoryEntity> history) {
        this.historyList = (history != null) ? history : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryEntity item = historyList.get(position);
        holder.tvDateTime.setText(item.getFormattedDateTime());
        holder.tvEventTitle.setText(item.getEventType());
        holder.tvModes.setText("Previous: " + item.getPreviousMode() + " → Changed to: " + item.getNewMode());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvEventTitle, tvModes;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvHistoryDateTime);
            tvEventTitle = itemView.findViewById(R.id.tvHistoryEventTitle);
            tvModes = itemView.findViewById(R.id.tvHistoryModes);
        }
    }
}
