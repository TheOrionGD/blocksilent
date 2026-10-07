package com.blocksilent.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.database.entities.NoiseSampleEntity;

import java.util.ArrayList;
import java.util.List;

public class NoiseSampleAdapter extends RecyclerView.Adapter<NoiseSampleAdapter.NoiseViewHolder> {

    private final Context context;
    private List<NoiseSampleEntity> samples = new ArrayList<>();

    public NoiseSampleAdapter(Context context) {
        this.context = context;
    }

    public void setSamples(List<NoiseSampleEntity> sampleList) {
        this.samples = sampleList != null ? sampleList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoiseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_noise_sample, parent, false);
        return new NoiseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoiseViewHolder holder, int position) {
        NoiseSampleEntity sample = samples.get(position);
        holder.tvZoneName.setText(sample.getZoneName() + " (" + sample.getNoiseCategory() + ")");
        holder.tvTimestamp.setText(sample.getFormattedTime());
        holder.tvDb.setText(String.format("%.1f dB", sample.getApproximateDb()));

        if (sample.getApproximateDb() < 40.0) {
            holder.tvDb.setTextColor(Color.parseColor("#4CAF50")); // Green: Quiet
        } else if (sample.getApproximateDb() <= 60.0) {
            holder.tvDb.setTextColor(Color.parseColor("#FFC107")); // Amber: Moderate
        } else {
            holder.tvDb.setTextColor(Color.parseColor("#F44336")); // Red: High noise
        }
    }

    @Override
    public int getItemCount() {
        return samples.size();
    }

    public static class NoiseViewHolder extends RecyclerView.ViewHolder {
        TextView tvZoneName, tvTimestamp, tvDb;

        public NoiseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvZoneName = itemView.findViewById(R.id.tvNoiseZoneName);
            tvTimestamp = itemView.findViewById(R.id.tvNoiseTimestamp);
            tvDb = itemView.findViewById(R.id.tvNoiseDbLevel);
        }
    }
}
