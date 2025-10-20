package com.example.gosortapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.os.Handler;
import android.os.Looper;

public class WeeklyAnalyticsModel {
    private static final String TAG = "WeeklyAnalyticsModel";
    private static final String PREF_NAME = "GoSort";
    private static WeeklyAnalyticsModel instance;
    private final List<Listener> listeners = new ArrayList<>();
    private final int[] values = new int[7]; // Last 7 days of data (0 = 6 days ago, 6 = today)
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();
    private final Context context;

    private WeeklyAnalyticsModel(Context context) {
        this.context = context.getApplicationContext();
        startFetchingStatistics();
    }

    public static synchronized WeeklyAnalyticsModel init(Context context) {
        if (instance == null) {
            instance = new WeeklyAnalyticsModel(context);
        }
        return instance;
    }

    public static synchronized WeeklyAnalyticsModel get() {
        if (instance == null) {
            throw new IllegalStateException("WeeklyAnalyticsModel must be initialized with context first");
        }
        return instance;
    }

    public interface Listener {
        void onDataChanged(int[] values);
    }

    public void addListener(Listener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
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
        // Initial fetch
        fetchStatistics();

        // Schedule periodic updates
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchStatistics();
                handler.postDelayed(this, 30000); // Fetch every 30 seconds
            }
        }, 30000);
    }

    private long normalizeToMidnightMillis(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private int daysBetween(Date earlier, Date later) {
        long e = normalizeToMidnightMillis(earlier);
        long l = normalizeToMidnightMillis(later);
        long diff = l - e;
        return (int) TimeUnit.MILLISECONDS.toDays(diff);
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

        String url = baseUrl + "statistics_api.php?type=daily_sorting&device_identity=" + deviceId;
        Log.d(TAG, "Fetching weekly statistics from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch weekly statistics", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Received weekly response: " + responseBody);

                    JSONObject json = new JSONObject(responseBody);
                    if (json.getBoolean("success")) {
                        JSONObject data = json.getJSONObject("data");
                        Log.d(TAG, "Parsed weekly data: " + data.toString());

                        // Reset values
                        for (int i = 0; i < values.length; i++) {
                            values[i] = 0;
                        }

                        // Prepare date parser (API returns dates in yyyy-MM-dd)
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getDefault());

                        // Today's date for reference
                        Date today = new Date();

                        // Iterate over keys and map counts into the 7-day window robustly
                        Iterator<String> keys = data.keys();
                        while (keys.hasNext()) {
                            String dateStr = keys.next();
                            try {
                                Date d = sdf.parse(dateStr);
                                if (d == null) continue;
                                int diff = daysBetween(d, today); // days from date to today
                                if (diff >= 0 && diff <= 6) {
                                    int index = 6 - diff; // 6 => today at index 6, 0 => 6 days ago at index 0
                                    values[index] = data.getInt(dateStr);
                                    Log.d(TAG, String.format("Mapped %s (-%d days) to index %d: %d items",
                                        dateStr, diff, index, values[index]));
                                }
                            } catch (Exception pe) {
                                Log.e(TAG, "Failed to parse date: " + dateStr, pe);
                            }
                        }

                        Log.d(TAG, "Weekly counts (0=6d ago .. 6=today): " +
                              values[0] + "," + values[1] + "," + values[2] + "," +
                              values[3] + "," + values[4] + "," + values[5] + "," + values[6]);

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
                    Log.e(TAG, "Error processing weekly response", e);
                }
            }
        });
    }
}
