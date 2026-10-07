package com.blocksilent.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.database.entities.WifiZoneEntity;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class WifiZoneAdapter extends RecyclerView.Adapter<WifiZoneAdapter.WifiZoneViewHolder> {

    public interface OnWifiZoneActionListener {
        void onToggleZone(WifiZoneEntity zone, boolean isEnabled);
        void onDeleteZone(WifiZoneEntity zone);
    }

    private final Context context;
    private final OnWifiZoneActionListener listener;
    private List<WifiZoneEntity> zoneList = new ArrayList<>();

    public WifiZoneAdapter(Context context, OnWifiZoneActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setZones(List<WifiZoneEntity> zones) {
        this.zoneList = zones != null ? zones : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WifiZoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wifi_zone, parent, false);
        return new WifiZoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiZoneViewHolder holder, int position) {
        WifiZoneEntity zone = zoneList.get(position);
        holder.tvRoomName.setText(zone.getRoomName() + " (Floor " + zone.getFloor() + ")");
        holder.tvBssid.setText("BSSID: " + zone.getBssid() + " | SSID: " + zone.getSsid());
        holder.tvPolicy.setText("Policy: " + zone.getSoundPolicy() + " | Block: " + zone.getBlockName());

        holder.switchZoneEnabled.setOnCheckedChangeListener(null);
        holder.switchZoneEnabled.setChecked(zone.isEnabled());

        holder.switchZoneEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleZone(zone, isChecked);
            }
        });

        holder.btnDeleteZone.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteZone(zone);
            }
        });
    }

    @Override
    public int getItemCount() {
        return zoneList.size();
    }

    public static class WifiZoneViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName, tvBssid, tvPolicy;
        SwitchMaterial switchZoneEnabled;
        ImageButton btnDeleteZone;

        public WifiZoneViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomName = itemView.findViewById(R.id.tvWifiRoomName);
            tvBssid = itemView.findViewById(R.id.tvWifiBssid);
            tvPolicy = itemView.findViewById(R.id.tvWifiPolicy);
            switchZoneEnabled = itemView.findViewById(R.id.switchWifiZoneEnabled);
            btnDeleteZone = itemView.findViewById(R.id.btnDeleteWifiZone);
        }
    }
}
