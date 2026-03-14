package com.GoSort.Application;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Handler;
import android.os.Looper;
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
import java.util.List;

public class NotificationsFragment extends Fragment {
    private static final String TAG = "NotificationsFragment";
    private static final String BASE_URL = "https://gosortweb-production.up.railway.app/api/";
    private static final int UPDATE_INTERVAL = 5000; // 5 seconds
    private RecyclerView rv;
    private NotificationAdapter adapter;
    private List<NotificationItem> data;
    private Handler updateHandler;
    private boolean isUpdating = false;
    private NotificationHelper notificationHelper;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUpdating && isAdded()) {
                checkBinFullness();
                updateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateHandler = new Handler(Looper.getMainLooper());
        notificationHelper = new NotificationHelper(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        isUpdating = true;
        updateHandler.post(updateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        isUpdating = false;
        updateHandler.removeCallbacks(updateRunnable);
    }

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
            showResolutionDialog(pos);
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

    private void showResolutionDialog(int position) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Issue Resolution")
            .setMessage("Has this issue been resolved?")
            .setPositiveButton("Yes", (dialog, which) -> {
                NotificationRepository.get().deleteNotification(position);
                adapter.notifyDataSetChanged();
            })
            .setNegativeButton("No", (dialog, which) -> {
                NotificationRepository.get().markRead(position);
                adapter.notifyItemChanged(position);

                NotificationDetailFragment detail = NotificationDetailFragment.newInstance(
                    data.get(position).message,
                    data.get(position).meta,
                    data.get(position).isHighPriority
                );
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detail)
                        .addToBackStack(null)
                        .commit();
            })
            .show();
    }

    private void checkBinFullness() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = requireContext().getSharedPreferences("GoSort", Context.MODE_PRIVATE);
                String deviceId = prefs.getString("sorter_device_id", "");

                Log.d(TAG, "Retrieved from prefs - Device ID: " + deviceId);

                if (deviceId.isEmpty()) {
                    Log.e(TAG, "Device ID not set in preferences");
                    return;
                }

                String apiUrl = BASE_URL + "bin_fullness.php?device_identity=" + deviceId;
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

                            requireActivity().runOnUiThread(() -> {
                                // Remove any existing notification for this bin first
                                removeExistingNotification(binName);

                                // Only create notification if bin is full or malfunctioning
                                if (fullness == -1) {
                                    String message = String.format("⚠️ Bin '%s' sensor MALFUNCTION detected! Please check the sensor immediately.",
                                        binName);
                                    String meta = String.format("© %s | Device: %s | Bin: %s | Status: Sensor Error",
                                        timestamp, deviceId, binName);

                                    NotificationItem notif = new NotificationItem(message, meta, true);
                                    data.add(0, notif);
                                    adapter.notifyDataSetChanged();
                                    Log.d(TAG, "Added malfunction notification for bin: " + binName);

                                    // Show system notification for malfunction
                                    notificationHelper.showBinAlert(
                                        "Bin Sensor Malfunction",
                                        String.format("Bin '%s' sensor is not responding properly. Please check!", binName)
                                    );
                                }
                                else if (fullness >= 90) {
                                    String message = String.format("Bin '%s' is FULL! Current fullness level is %d%%. Please empty immediately.",
                                        binName, fullness);
                                    String meta = String.format("© %s | Device: %s | Bin: %s | Fullness: %d%%",
                                        timestamp, deviceId, binName, fullness);

                                    NotificationItem notif = new NotificationItem(message, meta, true);
                                    data.add(0, notif);
                                    adapter.notifyDataSetChanged();
                                    Log.d(TAG, "Added fullness notification for bin: " + binName);

                                    // Show system notification for full bin
                                    notificationHelper.showBinAlert(
                                        "Bin Full Alert",
                                        String.format("Bin '%s' is %d%% full. Please empty!", binName, fullness)
                                    );
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking bin fullness: " + e.getMessage(), e);
            }
        }).start();
    }

    private void removeExistingNotification(String binName) {
        // Remove any existing notification for this bin
        for (int i = data.size() - 1; i >= 0; i--) {
            NotificationItem existing = data.get(i);
            if (existing.message.contains(binName)) {
                data.remove(i);
                break;
            }
        }
    }
}
