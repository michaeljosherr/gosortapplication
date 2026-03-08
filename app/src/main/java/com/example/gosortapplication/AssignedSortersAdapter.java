package com.example.gosortapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AssignedSortersAdapter extends RecyclerView.Adapter<AssignedSortersAdapter.ViewHolder> {

    private List<JSONObject> sorters = new ArrayList<>();
    private RecyclerView recyclerView;

    public AssignedSortersAdapter() {}

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assigned_sorter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            JSONObject sorter = sorters.get(position);

            holder.txtDeviceName.setText(sorter.optString("device_name", "Unknown Device"));
            holder.txtLocation.setText(sorter.optString("location", "Not specified"));

            // Online/offline from cached flag
            boolean isOnline = sorter.optBoolean("is_online", false);
            applyStatus(holder.txtStatus, isOnline);

            holder.txtBiodegPercent.setText(   sorter.optInt("biodegradable_fullness",     0) + "%");
            holder.txtNonBiodegPercent.setText(sorter.optInt("non_biodegradable_fullness", 0) + "%");
            holder.txtMixedPercent.setText(    sorter.optInt("mixed_fullness",             0) + "%");
            holder.txtHazardPercent.setText(   sorter.optInt("hazardous_fullness",         0) + "%");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() { return sorters.size(); }

    public void setSorters(List<JSONObject> sorters) {
        this.sorters = new ArrayList<>(sorters);
        notifyDataSetChanged();
    }

    /**
     * Updates bin percentages + online status directly on the visible ViewHolder —
     * no rebind, no blink.
     */
    public void updateCard(int index, int bio, int nonBio, int mixed, int hazardous, boolean isOnline) {
        if (index < 0 || index >= sorters.size()) return;

        // Cache values so they survive scroll/rebind
        try {
            JSONObject sorter = sorters.get(index);
            sorter.put("biodegradable_fullness",     bio);
            sorter.put("non_biodegradable_fullness", nonBio);
            sorter.put("mixed_fullness",             mixed);
            sorter.put("hazardous_fullness",         hazardous);
            sorter.put("is_online",                  isOnline);
        } catch (Exception ignored) {}

        // Update visible ViewHolder directly — skip notifyItemChanged to avoid blink
        if (recyclerView == null) return;
        ViewHolder vh = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(index);
        if (vh == null) return;

        vh.txtBiodegPercent.setText(bio       + "%");
        vh.txtNonBiodegPercent.setText(nonBio + "%");
        vh.txtMixedPercent.setText(mixed      + "%");
        vh.txtHazardPercent.setText(hazardous + "%");
        applyStatus(vh.txtStatus, isOnline);
    }

    private void applyStatus(TextView tv, boolean isOnline) {
        if (tv == null) return;
        if (isOnline) {
            tv.setText("Online");
            tv.setTextColor(0xFF14AE31);
        } else {
            tv.setText("Offline");
            tv.setTextColor(0xFFE74C3C);
        }
    }

    public void clear() {
        sorters.clear();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDeviceName, txtLocation, txtStatus;
        TextView txtBiodegPercent, txtNonBiodegPercent, txtMixedPercent, txtHazardPercent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDeviceName       = itemView.findViewById(R.id.txtDeviceName);
            txtLocation         = itemView.findViewById(R.id.txtLocation);
            txtStatus           = itemView.findViewById(R.id.txtStatus);
            txtBiodegPercent    = itemView.findViewById(R.id.txtBiodegPercent);
            txtNonBiodegPercent = itemView.findViewById(R.id.txtNonBiodegPercent);
            txtMixedPercent     = itemView.findViewById(R.id.txtMixedPercent);
            txtHazardPercent    = itemView.findViewById(R.id.txtHazardPercent);
        }
    }
}