package com.blocksilent.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blocksilent.app.R;
import com.blocksilent.app.database.entities.BlockEntity;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class BlockAdapter extends RecyclerView.Adapter<BlockAdapter.BlockViewHolder> {

    public interface OnBlockActionListener {
        void onEdit(BlockEntity block);
        void onDelete(BlockEntity block);
        void onToggleEnable(BlockEntity block, boolean enabled);
    }

    private final Context context;
    private List<BlockEntity> blockList = new ArrayList<>();
    private final OnBlockActionListener listener;

    public BlockAdapter(Context context, OnBlockActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setBlocks(List<BlockEntity> blocks) {
        this.blockList = (blocks != null) ? blocks : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_block, parent, false);
        return new BlockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockViewHolder holder, int position) {
        BlockEntity block = blockList.get(position);
        holder.tvBlockName.setText(block.getName());
        holder.tvBlockRadius.setText("Radius: " + (int) block.getRadius() + " m");

        String mode = block.getSoundMode();
        holder.tvBlockMode.setText(mode);
        if ("SILENT".equalsIgnoreCase(mode)) {
            holder.tvBlockMode.setBackgroundResource(R.drawable.bg_badge_silent);
            holder.tvBlockMode.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.mode_silent));
        } else if ("VIBRATE".equalsIgnoreCase(mode)) {
            holder.tvBlockMode.setBackgroundResource(R.drawable.bg_badge_vibrate);
            holder.tvBlockMode.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.mode_vibrate));
        } else {
            holder.tvBlockMode.setBackgroundResource(R.drawable.bg_badge_normal);
            holder.tvBlockMode.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.mode_normal));
        }

        if (block.isEnabled()) {
            holder.tvBlockStatus.setText("Active");
            holder.tvBlockStatus.setBackgroundResource(R.drawable.bg_badge_active);
            holder.tvBlockStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_active));
        } else {
            holder.tvBlockStatus.setText("Disabled");
            holder.tvBlockStatus.setBackgroundResource(R.drawable.bg_badge_inactive);
            holder.tvBlockStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_inactive));
        }

        holder.switchEnable.setOnCheckedChangeListener(null);
        holder.switchEnable.setChecked(block.isEnabled());

        holder.switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleEnable(block, isChecked);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(block);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(block);
        });
    }

    @Override
    public int getItemCount() {
        return blockList.size();
    }

    static class BlockViewHolder extends RecyclerView.ViewHolder {
        TextView tvBlockName, tvBlockMode, tvBlockRadius, tvBlockStatus;
        SwitchMaterial switchEnable;
        Button btnEdit, btnDelete;

        public BlockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBlockName = itemView.findViewById(R.id.tvBlockName);
            tvBlockMode = itemView.findViewById(R.id.tvBlockMode);
            tvBlockRadius = itemView.findViewById(R.id.tvBlockRadius);
            tvBlockStatus = itemView.findViewById(R.id.tvBlockStatus);
            switchEnable = itemView.findViewById(R.id.switchEnableBlock);
            btnEdit = itemView.findViewById(R.id.btnEditBlock);
            btnDelete = itemView.findViewById(R.id.btnDeleteBlock);
        }
    }
}
