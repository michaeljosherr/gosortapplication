package com.example.gosortapplication;

import android.content.SharedPreferences;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private static final String TAG = "NotificationsFragment";
    private RecyclerView rv;
    private NotificationAdapter adapter;
    private List<NotificationItem> data;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notifications, container, false);
        rv = v.findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        NotificationRepository repo = NotificationRepository.get();
        data = repo.getAll();

        // Check bin fullness when fragment is created
        checkBinFullness();

        adapter = new NotificationAdapter(data, (item, pos) -> {
            repo.markRead(pos);
            adapter.notifyItemChanged(pos);

            NotificationDetailFragment detail = NotificationDetailFragment.newInstance(item.message, item.meta, item.isHighPriority);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detail)
                    .addToBackStack(null)
                    .commit();
        });

        rv.setAdapter(adapter);
        View markAll = v.findViewById(R.id.tvMarkAll);
        if (markAll != null) {
            final NotificationRepository r = NotificationRepository.get();
            markAll.setOnClickListener(x -> {
                r.markAllRead();
                adapter.notifyDataSetChanged();
            });
        }
        return v;
    }

    private void checkBinFullness() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = requireContext().getSharedPreferences("GoSort", Context.MODE_PRIVATE);
                String ipAddress = prefs.getString("device_ip", "");
                String deviceId = prefs.getString("sorter_device_id", "");

                Log.d(TAG, "Retrieved from prefs - IP: " + ipAddress + ", Device ID: " + deviceId);

                if (ipAddress.isEmpty() || deviceId.isEmpty()) {
                    Log.e(TAG, "IP address or device ID not set in preferences");
                    return;
                }

                String apiUrl = "http://" + ipAddress + "/GoSort_Web/api/bin_fullness.php?device_identity=" + deviceId;
                Log.d(TAG, "Checking bin fullness API: " + apiUrl);

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    String jsonResponse = response.toString();
                    Log.d(TAG, "API Response: " + jsonResponse);

                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    if (jsonObject.getString("status").equals("success")) {
                        JSONArray readings = jsonObject.getJSONArray("data");

                        // Track latest reading per bin
                        java.util.HashMap<String, JSONObject> latestPerBin = new java.util.HashMap<>();

                        // Group latest readings by bin
                        for (int i = 0; i < readings.length(); i++) {
                            JSONObject reading = readings.getJSONObject(i);
                            String binName = reading.getString("bin_name");
                            if (!latestPerBin.containsKey(binName)) {
                                latestPerBin.put(binName, reading);
                            }
                        }

                        // Process each bin's latest reading
                        for (JSONObject latest : latestPerBin.values()) {
                            String binName = latest.getString("bin_name");
                            int fullness = latest.getInt("fullness_percentage");
                            String timestamp = latest.getString("timestamp");

                            Log.d(TAG, String.format("Latest reading for bin %s: %d%%", binName, fullness));

                            // Check if bin is full (>=90%)
                            if (fullness >= 90) {
                                String message = String.format("Bin '%s' is FULL! Current fullness level is %d%%. Please empty immediately.",
                                    binName, fullness);
                                String meta = String.format("© %s | Device: %s | Bin: %s | Fullness: %d%%",
                                    timestamp, deviceId, binName, fullness);

                                Log.d(TAG, "Creating notification for bin: " + binName + " at " + fullness + "% full");

                                requireActivity().runOnUiThread(() -> {
                                    NotificationItem notif = new NotificationItem(message, meta, true);
                                    boolean found = false;
                                    // Look for existing notification for this bin
                                    for (int i = 0; i < data.size(); i++) {
                                        NotificationItem existing = data.get(i);
                                        if (existing.message.contains(binName)) {
                                            data.set(i, notif); // Update existing
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        data.add(0, notif); // Add new at top
                                    }
                                    adapter.notifyDataSetChanged();
                                });
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking bin fullness: " + e.getMessage(), e);
            }
        }).start();
    }
}
