package com.example.gosortapplication;

import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class DeviceViewHolder extends RecyclerView.ViewHolder {
    private TextView deviceName;
    private TextView deviceLocation;

    public DeviceViewHolder(View itemView) {
        super(itemView);
        deviceName = itemView.findViewById(R.id.txtDeviceName);
        deviceLocation = itemView.findViewById(R.id.txtDeviceLocation);
    }

    public void bind(String name, String location) {
        deviceName.setText(name);
        deviceLocation.setText(location);
    }

    public void setSelected(boolean selected) {
        // Change background color or add visual indicator for selected state
        if (selected) {
            itemView.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E8")); // Light green background
        } else {
            itemView.setBackgroundColor(android.graphics.Color.parseColor("#F8FFEF")); // Default background
        }
    }
}
