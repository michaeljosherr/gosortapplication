package com.example.gosortapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.ActivityViewHolder> {
    private List<ActivityLog> activityLogs;

    public ActivityLogAdapter(List<ActivityLog> activityLogs) {
        this.activityLogs = activityLogs;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_log, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityLog log = activityLogs.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return activityLogs.size();
    }

    public void setActivityLogs(List<ActivityLog> logs) {
        this.activityLogs = logs;
        notifyDataSetChanged();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView message;
        TextView timeAgo;
        TextView deviceName;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.activityIcon);
            message = itemView.findViewById(R.id.activityMessage);
            timeAgo = itemView.findViewById(R.id.activityTimeAgo);
            deviceName = itemView.findViewById(R.id.activityDeviceName);
        }

        void bind(ActivityLog log) {
            icon.setImageResource(log.getIconResId());
            message.setText(log.getMessage());
            timeAgo.setText(log.getTimeAgo());
            deviceName.setText("(" + log.getDeviceName() + ")");
        }
    }
}

