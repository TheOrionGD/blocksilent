package com.blocksilent.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.database.entities.AutomationDecisionEntity;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryItemClickListener {
        void onItemClick(AutomationDecisionEntity item);
    }

    private final Context context;
    private List<AutomationDecisionEntity> decisionList = new ArrayList<>();
    private final OnHistoryItemClickListener listener;

    public HistoryAdapter(Context context, OnHistoryItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setDecisions(List<AutomationDecisionEntity> decisions) {
        this.decisionList = (decisions != null) ? decisions : new ArrayList<>();
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
        AutomationDecisionEntity item = decisionList.get(position);

        holder.tvDateTime.setText(item.getFormattedTime());
        holder.tvEventTitle.setText(item.getLocation());
        holder.tvReason.setText(item.getReason());
        holder.tvModes.setText(item.getPreviousMode() + " → " + item.getNewMode());
        holder.tvPriority.setText("Priority: " + item.getPriority());

        // Mode badge styling
        if ("SILENT".equalsIgnoreCase(item.getNewMode())) {
            holder.tvModes.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_silent));
            holder.tvModes.setTextColor(ContextCompat.getColor(context, R.color.status_silent));
        } else if ("VIBRATE".equalsIgnoreCase(item.getNewMode())) {
            holder.tvModes.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_vibrate));
            holder.tvModes.setTextColor(ContextCompat.getColor(context, R.color.status_vibrate));
        } else {
            holder.tvModes.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_normal));
            holder.tvModes.setTextColor(ContextCompat.getColor(context, R.color.primary));
        }

        // Source icon based on decision trigger
        String source = item.getSource() != null ? item.getSource().toUpperCase() : "";
        if (source.contains("GEO")) {
            holder.ivSourceIcon.setImageResource(R.drawable.ic_location);
            holder.ivSourceIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary));
        } else if (source.contains("WIFI")) {
            holder.ivSourceIcon.setImageResource(R.drawable.ic_wifi);
            holder.ivSourceIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_active));
        } else if (source.contains("SENSOR") || source.contains("FLIP")) {
            holder.ivSourceIcon.setImageResource(R.drawable.ic_sensor);
            holder.ivSourceIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_vibrate));
        } else if (source.contains("EMERGENCY") || source.contains("SIREN")) {
            holder.ivSourceIcon.setImageResource(R.drawable.ic_warning);
            holder.ivSourceIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_inactive));
        } else if (source.contains("TIMETABLE")) {
            holder.ivSourceIcon.setImageResource(R.drawable.ic_timetable);
            holder.ivSourceIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary));
        } else {
            holder.ivSourceIcon.setImageResource(R.drawable.ic_history);
            holder.ivSourceIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return decisionList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSourceIcon;
        TextView tvEventTitle, tvDateTime, tvReason, tvModes, tvPriority;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSourceIcon = itemView.findViewById(R.id.ivHistorySourceIcon);
            tvEventTitle = itemView.findViewById(R.id.tvHistoryEventTitle);
            tvDateTime = itemView.findViewById(R.id.tvHistoryDateTime);
            tvReason = itemView.findViewById(R.id.tvHistoryReason);
            tvModes = itemView.findViewById(R.id.tvHistoryModes);
            tvPriority = itemView.findViewById(R.id.tvHistoryPriority);
        }
    }
}
