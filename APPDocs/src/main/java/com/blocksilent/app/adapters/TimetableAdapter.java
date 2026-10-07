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
import com.blocksilent.app.database.entities.TimetableEntity;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder> {

    public interface OnTimetableActionListener {
        void onDelete(TimetableEntity timetable);
        void onToggleEnable(TimetableEntity timetable, boolean enabled);
    }

    private final Context context;
    private List<TimetableEntity> timetableList = new ArrayList<>();
    private final OnTimetableActionListener listener;

    public TimetableAdapter(Context context, OnTimetableActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setTimetables(List<TimetableEntity> timetables) {
        this.timetableList = (timetables != null) ? timetables : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimetableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_timetable, parent, false);
        return new TimetableViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimetableViewHolder holder, int position) {
        TimetableEntity item = timetableList.get(position);
        holder.tvSubject.setText(item.getSubjectName());
        String mode = item.getSoundMode();
        holder.tvBlockMode.setText(item.getBlockName() + " → " + mode);
        if ("SILENT".equalsIgnoreCase(mode)) {
            holder.tvBlockMode.setBackgroundResource(R.drawable.bg_badge_silent);
            holder.tvBlockMode.setTextColor(android.graphics.Color.parseColor("#8B5CF6"));
        } else if ("VIBRATE".equalsIgnoreCase(mode)) {
            holder.tvBlockMode.setBackgroundResource(R.drawable.bg_badge_vibrate);
            holder.tvBlockMode.setTextColor(android.graphics.Color.parseColor("#06B6D4"));
        } else {
            holder.tvBlockMode.setBackgroundResource(R.drawable.bg_badge_normal);
            holder.tvBlockMode.setTextColor(android.graphics.Color.parseColor("#10B981"));
        }

        holder.switchEnable.setOnCheckedChangeListener(null);
        holder.switchEnable.setChecked(item.isEnabled());

        holder.switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onToggleEnable(item, isChecked);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return timetableList.size();
    }

    static class TimetableViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvDayTime, tvBlockMode;
        SwitchMaterial switchEnable;
        Button btnDelete;

        public TimetableViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubjectName);
            tvDayTime = itemView.findViewById(R.id.tvDayTime);
            tvBlockMode = itemView.findViewById(R.id.tvBlockMode);
            switchEnable = itemView.findViewById(R.id.switchEnableTimetable);
            btnDelete = itemView.findViewById(R.id.btnDeleteTimetable);
        }
    }
}
