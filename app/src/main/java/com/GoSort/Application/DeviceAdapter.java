package com.GoSort.Application;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceViewHolder> {
    private List<JSONObject> devices = new ArrayList<>();
    private OnDeviceClickListener onDeviceClickListener;
    private int selectedPosition = -1;

    public interface OnDeviceClickListener {
        void onDeviceClick(JSONObject device);
    }

    public DeviceAdapter(OnDeviceClickListener listener) {
        this.onDeviceClickListener = listener;
    }

    public void setDevices(List<JSONObject> deviceList) {
        this.devices = deviceList;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int previousSelected = selectedPosition;
        selectedPosition = position;
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }

    public List<JSONObject> getDevices() {
        return devices;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        android.view.View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        JSONObject device = devices.get(position);
        try {
            String deviceName = device.optString("device_name", "Device");
            String location = device.optString("location", "Unknown");
            holder.bind(deviceName, location);
            holder.setSelected(selectedPosition == position);
            holder.itemView.setOnClickListener(v -> {
                if (onDeviceClickListener != null) {
                    onDeviceClickListener.onDeviceClick(device);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }
}
