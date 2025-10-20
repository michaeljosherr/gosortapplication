package com.example.gosortapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

public class AnalyticsModel {
    private static final String TAG = "AnalyticsModel";
    private static final String PREF_NAME = "GoSort";
    private static AnalyticsModel instance;
    private final List<Listener> listeners = new ArrayList<>();
    private final List<ActivityListener> activityListeners = new ArrayList<>();
    private final int[] values = new int[]{0, 0, 0, 0}; // Reduced to 4 categories
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();
    private final Context context;

    private AnalyticsModel(Context context) {
        this.context = context.getApplicationContext();
        startFetchingStatistics();
    }

    public static synchronized AnalyticsModel init(Context context) {
        if (instance == null) {
            instance = new AnalyticsModel(context);
        }
        return instance;
    }

    public static synchronized AnalyticsModel get() {
        if (instance == null) {
            throw new IllegalStateException("AnalyticsModel must be initialized with context first");
        }
        return instance;
    }

    public interface Listener {
        void onDataChanged(int[] values);
    }

    // New listener interface for weekly activity (7 days)
    public interface ActivityListener {
        void onWeeklyActivity(int[] counts);
    }

    public void addListener(Listener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void addActivityListener(ActivityListener l) {
        if (!activityListeners.contains(l)) {
            activityListeners.add(l);
        }
    }

    public void removeActivityListener(ActivityListener l) {
        activityListeners.remove(l);
    }

    public int[] getValues() {
        return values.clone();
    }

    private String getBaseUrl() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String ip = prefs.getString("device_ip", "");
        Log.d(TAG, "Retrieved device_ip from SharedPreferences: " + ip);
        return "http://" + ip + "/GoSort_Web/api/";
    }

    private String getDeviceIdentity() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String deviceId = prefs.getString("sorter_device_id", "");
        Log.d(TAG, "Retrieved sorter_device_id from SharedPreferences: " + deviceId);
        return deviceId;
    }

    private void startFetchingStatistics() {
        // Initial fetch both types
        fetchStatistics();
        fetchWeeklyActivity();

        // Schedule periodic updates
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchStatistics();
                fetchWeeklyActivity();
                handler.postDelayed(this, 30000); // Fetch every 30 seconds
            }
        }, 30000);
    }

    private void fetchStatistics() {
        String deviceId = getDeviceIdentity();
        if (deviceId.isEmpty()) {
            Log.e(TAG, "Device identity not found in SharedPreferences");
            return;
        }

        String baseUrl = getBaseUrl();
        if (baseUrl.startsWith("http://:/")) {
            Log.e(TAG, "Invalid IP address in SharedPreferences");
            return;
        }

        String url = baseUrl + "statistics_api.php?type=trash_types&device_identity=" + deviceId;
        Log.d(TAG, "Fetching statistics from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch statistics", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    ResponseBody body = response.body();
                    if (body == null) {
                        Log.e(TAG, "Empty response body");
                        return;
                    }

                    String responseBody = body.string();
                    Log.d(TAG, "Received response: " + responseBody);

                    JSONObject json = new JSONObject(responseBody);
                    if (json.getBoolean("success")) {
                        JSONObject data = json.getJSONObject("data");
                        Log.d(TAG, "Parsed statistics data: " + data);

                        // Reset values
                        Arrays.fill(values, 0);

                        // Update values from API response - only 4 trash categories
                        if (data.has("biodegradable")) values[0] = data.optInt("biodegradable", 0);
                        if (data.has("non-biodegradable")) values[1] = data.optInt("non-biodegradable", 0);
                        if (data.has("mixed")) values[2] = data.optInt("mixed", 0);
                        if (data.has("hazardous")) values[3] = data.optInt("hazardous", 0);

                        // Convert to percentages
                        int total = 0;
                        for (int value : values) total += value;

                        if (total > 0) {
                            for (int i = 0; i < values.length; i++) {
                                values[i] = (values[i] * 100) / total;
                            }
                        }

                        Log.d(TAG, "Calculated percentages: " +
                              "Bio=" + values[0] + "%, " +
                              "NonBio=" + values[1] + "%, " +
                              "Mixed=" + values[2] + "%, " +
                              "Hazardous=" + values[3] + "%");

                        // Notify listeners on UI thread
                        handler.post(() -> {
                            for (Listener l : listeners) {
                                l.onDataChanged(values.clone());
                            }
                        });
                    } else {
                        Log.e(TAG, "API returned error: " + json.optString("error", "Unknown error"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                }
            }
        });
    }

    // Fetch weekly/daily activity (last 7 days) and notify activityListeners
    private void fetchWeeklyActivity() {
        String deviceId = getDeviceIdentity();
        if (deviceId.isEmpty()) {
            Log.e(TAG, "Device identity not found in SharedPreferences for weekly activity");
            return;
        }

        String baseUrl = getBaseUrl();
        if (baseUrl.startsWith("http://:/")) {
            Log.e(TAG, "Invalid IP address in SharedPreferences");
            return;
        }

        String url = baseUrl + "statistics_api.php?type=daily_sorting&device_identity=" + deviceId;
        Log.d(TAG, "Fetching weekly activity from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch weekly activity", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    ResponseBody body = response.body();
                    if (body == null) {
                        Log.e(TAG, "Empty response body for weekly activity");
                        return;
                    }

                    String responseBody = body.string();
                    Log.d(TAG, "Received weekly response: " + responseBody);

                    JSONObject json = new JSONObject(responseBody);
                    if (json.getBoolean("success")) {
                        JSONObject data = json.optJSONObject("data");
                        // Prepare counts for 7 days: day0 = 6 days ago, ..., day6 = today
                        int[] counts = new int[7];
                        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        Calendar cal = Calendar.getInstance();

                        for (int i = 0; i < 7; i++) {
                            // compute date = today - (6 - i)
                            Calendar c = (Calendar) cal.clone();
                            c.add(Calendar.DAY_OF_YEAR, -(6 - i));
                            String key = fmt.format(c.getTime());
                            if (data != null && data.has(key)) {
                                counts[i] = data.optInt(key, 0);
                            } else {
                                counts[i] = 0;
                            }
                        }

                        Log.d(TAG, "Weekly counts: " + Arrays.toString(counts));

                        handler.post(() -> {
                            for (ActivityListener al : activityListeners) {
                                al.onWeeklyActivity(counts.clone());
                            }
                        });

                    } else {
                        Log.e(TAG, "Weekly API returned error: " + json.optString("error", "Unknown error"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing weekly activity response", e);
                }
            }
        });
    }
}
