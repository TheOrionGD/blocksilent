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
            holder.tvBlockMode.setTextColor(Color.parseColor("#8B5CF6"));
        } else if ("VIBRATE".equalsIgnoreCase(mode)) {
            holder.tvBlockMode.setBackgroundResource(R.drawable.bg_badge_vibrate);
            holder.tvBlockMode.setTextColor(Color.parseColor("#06B6D4"));
        } else {
            holder.tvBlockMode.setBackgroundResource(R.drawable.bg_badge_normal);
            holder.tvBlockMode.setTextColor(Color.parseColor("#10B981"));
        }

        if (block.isEnabled()) {
            holder.tvBlockStatus.setText("Active");
            holder.tvBlockStatus.setBackgroundResource(R.drawable.bg_badge_active);
            holder.tvBlockStatus.setTextColor(Color.parseColor("#059669"));
        } else {
            holder.tvBlockStatus.setText("Disabled");
            holder.tvBlockStatus.setBackgroundResource(R.drawable.bg_badge_inactive);
            holder.tvBlockStatus.setTextColor(Color.parseColor("#DC2626"));
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
