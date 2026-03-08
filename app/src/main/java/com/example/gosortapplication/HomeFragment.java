package com.example.gosortapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.content.Context;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements DeviceAdapter.OnDeviceClickListener {
    private static final String TAG = "HomeFragment";
    private static final String PREF_NAME = "GoSort";
    private static final String BASE_URL = "https://web-production-15f71.up.railway.app/api/";
    // Note: BinFullnessApi uses GoSort_Sorters.php at the root, not /api/

    // Views
    private TextView greetingText;
    private TextView assignedAreaText;
    private TextView txtBinStatusDeviceName;

    // Bin progress indicators
    private CircularProgressIndicator circBiodeg;
    private CircularProgressIndicator circNonBiodeg;
    private CircularProgressIndicator circMixed;
    private CircularProgressIndicator circHazardous;

    // Bin percent text views (center of circle)
    private TextView txtBiodegPercent;
    private TextView txtNonBiodegPercent;
    private TextView txtMixedPercent;
    private TextView txtHazardousPercent;

    // Bin status text views
    private TextView txtBiodegStatus;
    private TextView txtNonBiodegStatus;
    private TextView txtMixedStatus;
    private TextView txtHazardousStatus;

    // Bin priority text views
    private TextView txtBiodegPriority;
    private TextView txtNonBiodegPriority;
    private TextView txtMixedPriority;
    private TextView txtHazardousPriority;

    // Handler and API
    private Handler handler;
    private Runnable updateRunnable;
    private BinFullnessApi binFullnessApi;
    private DeviceAdapter deviceAdapter;
    private String currentDeviceId;
    private String currentDeviceName;

    // Recent Activity
    private List<ActivityLog> activityLogs;
    private ActivityLogAdapter activityLogAdapter;
    private int previousBiodegFullness = -1;
    private int previousNonBiodegFullness = -1;
    private int previousMixedFullness = -1;
    private int previousHazardousFullness = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            initializeViews(root);

            binFullnessApi = new BinFullnessApi("");

            activityLogs = new ArrayList<>();
            activityLogAdapter = new ActivityLogAdapter(activityLogs);

            greetingText.setText("Hey!");
            assignedAreaText.setText("View your assigned area/device:");

            setupDeviceRecyclerView(root);
            setupRecentActivityRecyclerView(root);

            updateBinUI(circBiodeg, txtBiodegPercent, txtBiodegStatus, txtBiodegPriority, 0);
            updateBinUI(circNonBiodeg, txtNonBiodegPercent, txtNonBiodegStatus, txtNonBiodegPriority, 0);
            updateBinUI(circMixed, txtMixedPercent, txtMixedStatus, txtMixedPriority, 0);
            updateBinUI(circHazardous, txtHazardousPercent, txtHazardousStatus, txtHazardousPriority, 0);

            setupPeriodicUpdates();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing: " + e.getMessage());
        }

        return root;
    }

    private void initializeViews(View root) {
        greetingText = root.findViewById(R.id.greetingText);
        assignedAreaText = root.findViewById(R.id.assignedAreaText);
        txtBinStatusDeviceName = root.findViewById(R.id.txtBinStatusDeviceName);

        circBiodeg = root.findViewById(R.id.circBiodeg);
        circNonBiodeg = root.findViewById(R.id.circNonBiodeg);
        circMixed = root.findViewById(R.id.circMixed);
        circHazardous = root.findViewById(R.id.circHazardous);

        // Percent text views (center of circle)
        txtBiodegPercent = root.findViewById(R.id.txtBiodegPercent);
        txtNonBiodegPercent = root.findViewById(R.id.txtNonBiodegPercent);
        txtMixedPercent = root.findViewById(R.id.txtMixedPercent);
        txtHazardousPercent = root.findViewById(R.id.txtHazardousPercent);

        txtBiodegStatus = root.findViewById(R.id.txtBiodegStatus);
        txtNonBiodegStatus = root.findViewById(R.id.txtNonBiodegStatus);
        txtMixedStatus = root.findViewById(R.id.txtMixedStatus);
        txtHazardousStatus = root.findViewById(R.id.txtHazardousStatus);

        txtBiodegPriority = root.findViewById(R.id.txtBiodegPriority);
        txtNonBiodegPriority = root.findViewById(R.id.txtNonBiodegPriority);
        txtMixedPriority = root.findViewById(R.id.txtMixedPriority);
        txtHazardousPriority = root.findViewById(R.id.txtHazardousPriority);
    }

    private void setupDeviceRecyclerView(View root) {
        RecyclerView recyclerDevices = root.findViewById(R.id.recyclerDevices);
        if (recyclerDevices == null) {
            Log.e(TAG, "recyclerDevices not found");
            return;
        }

        deviceAdapter = new DeviceAdapter(this);
        recyclerDevices.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        recyclerDevices.setAdapter(deviceAdapter);

        fetchAssignedDevices();
    }

    private void fetchAssignedDevices() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");

        if (username.isEmpty()) {
            Log.e(TAG, "No username in SharedPreferences");
            return;
        }

        Log.d(TAG, "Fetching devices for: " + username);

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "user_details_api.php?username=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getResponseCode() == 200
                                ? conn.getInputStream()
                                : conn.getErrorStream()));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();

                Log.d(TAG, "user_details response: " + response);

                JSONObject json = new JSONObject(response.toString());

                if (json.optBoolean("success", false)) {
                    JSONArray assignedSorters = json.getJSONObject("data")
                            .getJSONArray("assigned_sorters");

                    Log.d(TAG, "Devices found: " + assignedSorters.length());

                    List<JSONObject> deviceList = new ArrayList<>();
                    for (int i = 0; i < assignedSorters.length(); i++) {
                        deviceList.add(assignedSorters.getJSONObject(i));
                    }

                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        deviceAdapter.setDevices(deviceList);
                        if (!deviceList.isEmpty()) {
                            onDeviceClick(deviceList.get(0));
                        }
                    });
                } else {
                    Log.e(TAG, "API error: " + json.optString("error"));
                }

            } catch (Exception e) {
                Log.e(TAG, "fetchAssignedDevices error: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void onDeviceClick(JSONObject device) {
        try {
            currentDeviceId = device.optString("device_identity", "");
            currentDeviceName = device.optString("device_name", "Unknown Device");
            Log.d(TAG, "Device selected: " + currentDeviceId + " - " + currentDeviceName);

            for (int i = 0; i < deviceAdapter.getItemCount(); i++) {
                if (deviceAdapter.getDevices().get(i)
                        .optString("device_identity", "").equals(currentDeviceId)) {
                    deviceAdapter.setSelectedPosition(i);
                    break;
                }
            }

            if (txtBinStatusDeviceName != null) {
                txtBinStatusDeviceName.setText(currentDeviceName);
            }

            updateBinFullness();

        } catch (Exception e) {
            Log.e(TAG, "onDeviceClick error: " + e.getMessage());
        }
    }

    private void setupPeriodicUpdates() {
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUserInfo();
                if (currentDeviceId != null && !currentDeviceId.isEmpty()) {
                    updateBinFullness();
                }
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updateRunnable);
    }

    private void updateBinFullness() {
        if (getView() == null) return;

        String deviceId = currentDeviceId;
        if (deviceId == null || deviceId.isEmpty()) {
            SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            deviceId = prefs.getString("sorter_device_id", "");
        }

        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "No device ID available");
            return;
        }

        final String finalDeviceId = deviceId;
        binFullnessApi.getBinFullness(finalDeviceId, new BinFullnessApi.BinFullnessCallback() {
            @Override
            public void onSuccess(JSONArray binData) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    try {
                        // FIX: store by normalized bin_name so lookup keys match correctly
                        JSONObject latestBinData = new JSONObject();
                        for (int i = 0; i < binData.length(); i++) {
                            JSONObject bin = binData.getJSONObject(i);
                            String rawName = bin.getString("bin_name").toLowerCase().trim();
                            String normalizedKey = normalizeBinName(rawName);
                            Log.d(TAG, "Bin from API: raw='" + rawName + "' normalized='" + normalizedKey + "'");
                            if (!latestBinData.has(normalizedKey)) {
                                latestBinData.put(normalizedKey, bin);
                            }
                        }

                        updateBinTypeIfExists(latestBinData, "bio", circBiodeg, txtBiodegPercent, txtBiodegStatus, txtBiodegPriority);
                        updateBinTypeIfExists(latestBinData, "non-bio", circNonBiodeg, txtNonBiodegPercent, txtNonBiodegStatus, txtNonBiodegPriority);
                        updateBinTypeIfExists(latestBinData, "mixed", circMixed, txtMixedPercent, txtMixedStatus, txtMixedPriority);
                        updateBinTypeIfExists(latestBinData, "hazardous", circHazardous, txtHazardousPercent, txtHazardousStatus, txtHazardousPriority);

                    } catch (Exception e) {
                        Log.e(TAG, "Error updating bin UI: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Bin fullness error: " + message);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateBinUI(circBiodeg, txtBiodegPercent, txtBiodegStatus, txtBiodegPriority, -1);
                        updateBinUI(circNonBiodeg, txtNonBiodegPercent, txtNonBiodegStatus, txtNonBiodegPriority, -1);
                        updateBinUI(circMixed, txtMixedPercent, txtMixedStatus, txtMixedPriority, -1);
                        updateBinUI(circHazardous, txtHazardousPercent, txtHazardousStatus, txtHazardousPriority, -1);
                    });
                }
            }
        });
    }

    /**
     * Normalizes bin_name from API ("Non-Bio", "Bio", "Hazardous", "Mixed")
     * to internal keys ("non-bio", "bio", "hazardous", "mixed")
     */
    private String normalizeBinName(String rawName) {
        switch (rawName) {
            case "Non-Bio": return "non-bio";
            case "Bio":     return "bio";
            case "Hazardous": return "hazardous";
            case "Mixed":   return "mixed";
            default:        return rawName.toLowerCase().trim();
        }
    }

    private void updateBinTypeIfExists(JSONObject latestBinData, String binType,
                                       CircularProgressIndicator progressIndicator,
                                       TextView percentText,
                                       TextView statusText, TextView priorityText) {
        try {
            if (latestBinData.has(binType)) {
                int fullness = latestBinData.getJSONObject(binType).getInt("fullness_percentage");
                trackBinChanges(binType, fullness);
                updateBinUI(progressIndicator, percentText, statusText, priorityText, fullness);
            } else {
                Log.w(TAG, "No data found for bin type: " + binType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating " + binType + ": " + e.getMessage());
        }
    }

    private void trackBinChanges(String binType, int newFullness) {
        if (currentDeviceName == null || currentDeviceName.isEmpty()) return;
        int prev = getPreviousFullness(binType);

        // First reading — log the current state regardless of level
        if (prev == -1) {
            setPreviousFullness(binType, newFullness);
            String currentStatus;
            if (newFullness <= 25) currentStatus = "is Low (" + newFullness + "%)";
            else if (newFullness <= 50) currentStatus = "is Medium (" + newFullness + "%)";
            else if (newFullness <= 75) currentStatus = "is Nearly Full (" + newFullness + "%)";
            else currentStatus = "is Full (" + newFullness + "%)";
            addActivityLog(currentDeviceName, binType,
                    getBinDisplayName(binType) + " " + currentStatus, R.drawable.ic_analytics);
            return;
        }

        setPreviousFullness(binType, newFullness);

        // Log threshold crossings going up
        if (newFullness >= 50 && prev < 50)
            addActivityLog(currentDeviceName, binType, getBinDisplayName(binType) + " reached 50%", R.drawable.ic_analytics);
        if (newFullness >= 90 && prev < 90)
            addActivityLog(currentDeviceName, binType, getBinDisplayName(binType) + " reached 90%", R.drawable.ic_analytics);
        if (newFullness >= 100 && prev < 100)
            addActivityLog(currentDeviceName, binType, getBinDisplayName(binType) + " is Full!", R.drawable.ic_analytics);

        // Log when bin is emptied
        if (newFullness <= 10 && prev > 10)
            addActivityLog(currentDeviceName, binType, getBinDisplayName(binType) + " was emptied", R.drawable.ic_analytics);
    }

    private int getPreviousFullness(String binType) {
        switch (binType) {
            case "bio": return previousBiodegFullness;
            case "non-bio": return previousNonBiodegFullness;
            case "mixed": return previousMixedFullness;
            case "hazardous": return previousHazardousFullness;
            default: return -1;
        }
    }

    private void setPreviousFullness(String binType, int fullness) {
        switch (binType) {
            case "bio": previousBiodegFullness = fullness; break;
            case "non-bio": previousNonBiodegFullness = fullness; break;
            case "mixed": previousMixedFullness = fullness; break;
            case "hazardous": previousHazardousFullness = fullness; break;
        }
    }

    private String getBinDisplayName(String binType) {
        switch (binType) {
            case "bio": return "Biodegradable Bin";
            case "non-bio": return "Non-Biodegradable Bin";
            case "mixed": return "Mixed Waste Bin";
            case "hazardous": return "Hazardous Bin";
            default: return "Bin";
        }
    }

    private void addActivityLog(String deviceName, String binType, String message, int iconResId) {
        ActivityLog log = new ActivityLog(deviceName, binType, message, System.currentTimeMillis(), iconResId);
        activityLogs.add(0, log);
        if (activityLogs.size() > 5) activityLogs.remove(activityLogs.size() - 1);
        activityLogAdapter.notifyDataSetChanged();
        saveActivityLogs();
    }

    private void saveActivityLogs() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray();
            for (ActivityLog log : activityLogs) arr.put(log.toJSONObject());
            prefs.edit().putString("recent_activity", arr.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving logs: " + e.getMessage());
        }
    }

    private void updateBinUI(CircularProgressIndicator progressIndicator,
                             TextView percentText,
                             TextView statusText, TextView priorityText, int fullness) {
        if (progressIndicator == null || statusText == null || priorityText == null) return;

        progressIndicator.setIndeterminate(false);
        progressIndicator.setMax(100);

        if (fullness == -1) {
            progressIndicator.setProgress(0);
            if (percentText != null) percentText.setText("N/A");
            statusText.setText("Unavailable");
            priorityText.setText("No signal");
            progressIndicator.setIndicatorColor(android.graphics.Color.parseColor("#BDBDBD"));
            statusText.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
            priorityText.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
            return;
        }

        int value = Math.max(0, Math.min(100, fullness));
        String status; String priority; int priorityColor;

        if (value <= 10) {
            status = "Bin is Empty";
            priority = "Lowest Priority";
            priorityColor = android.graphics.Color.parseColor("#4CAF50");

        } else if (value <= 50) {
            status = "Bin is Partially Full";
            priority = "Low Priority";
            priorityColor = android.graphics.Color.parseColor("#4CAF50");

        } else if (value <= 75) {
            status = "Bin is Nearly Full";
            priority = "Medium Priority";
            priorityColor = android.graphics.Color.parseColor("#FF9800");

        } else {
            status = "Bin is Full";
            priority = "High Priority";
            priorityColor = android.graphics.Color.parseColor("#D50000");
        }

        progressIndicator.setProgress(value);

        // FIX: update the percentage text in the center of the circle
        if (percentText != null) percentText.setText(value + "%");

        statusText.setText(status);
        priorityText.setText(priority);
        statusText.setTextColor(android.graphics.Color.parseColor("#000000"));
        priorityText.setTextColor(priorityColor);
    }

    private void updateUserInfo() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, getActivity().MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String lastName = prefs.getString("lastName", "");
        try {
            if (greetingText != null) {
                String displayName = lastName.isEmpty() ? username : username + " " + lastName;
                greetingText.setText(String.format("Hey, %s!", displayName));
            }
        } catch (Exception e) {
            Log.e(TAG, "updateUserInfo error: " + e.getMessage());
        }
    }

    private void setupRecentActivityRecyclerView(View root) {
        RecyclerView recycler = root.findViewById(R.id.recyclerRecentActivity);
        if (recycler == null) {
            Log.e(TAG, "recyclerRecentActivity not found");
            return;
        }
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(activityLogAdapter);
        loadActivityLogs();
    }

    private void loadActivityLogs() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            String json = prefs.getString("recent_activity", "");
            if (!json.isEmpty()) {
                JSONArray arr = new JSONArray(json);
                activityLogs.clear();
                for (int i = 0; i < arr.length(); i++)
                    activityLogs.add(new ActivityLog(arr.getJSONObject(i)));
                activityLogAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "loadActivityLogs error: " + e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
}