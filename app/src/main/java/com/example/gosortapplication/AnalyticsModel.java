package com.example.gosortapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AnalyticsModel {
    private static final String TAG = "AnalyticsModel";
    private static final String BASE_URL = "https://web-production-15f71.up.railway.app/api/";

    private static AnalyticsModel instance;

    private final List<DailyListener> dailyListeners = new ArrayList<>();

    // Cached today data: bio, nonbio, mixed, hazardous
    private int[] todayCounts = new int[]{0, 0, 0, 0};
    private int   todayTotal  = 0;

    private String currentDeviceId = "";

    private final Handler     handler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();
    private final Context     context;

    private AnalyticsModel(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized AnalyticsModel init(Context context) {
        if (instance == null) instance = new AnalyticsModel(context);
        return instance;
    }

    public static synchronized AnalyticsModel get() {
        if (instance == null) throw new IllegalStateException("AnalyticsModel not initialized");
        return instance;
    }

    // ─── Interface ───────────────────────────────────────────────────────────

    public interface DailyListener {
        /** counts[0]=bio, [1]=nonbio, [2]=mixed, [3]=hazardous */
        void onDailyData(int[] counts, int[] percentages, int total);
    }

    public void addDailyListener(DailyListener l)    { if (!dailyListeners.contains(l)) dailyListeners.add(l); }
    public void removeDailyListener(DailyListener l) { dailyListeners.remove(l); }

    public int[] getTodayCounts()      { return todayCounts.clone(); }
    public int   getTodayTotal()       { return todayTotal; }

    // ─── Fetch ───────────────────────────────────────────────────────────────

    public void fetch(String deviceId) {
        currentDeviceId = deviceId != null ? deviceId : "";
        if (currentDeviceId.isEmpty()) { Log.e(TAG, "Empty deviceId"); return; }
        fetchTodayStats();
    }

    private void fetchTodayStats() {
        String url = BASE_URL + "get_daily_sorting.php";
        Log.d(TAG, "Fetching today: " + url);

        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Today fetch failed", e);
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    ResponseBody body = response.body();
                    if (body == null) return;
                    String raw = body.string();
                    Log.d(TAG, "Today response: " + raw);

                    JSONObject json = new JSONObject(raw);
                    if (!json.optBoolean("success", false)) return;

                    JSONArray data  = json.getJSONArray("data");
                    int[] counts    = new int[4];

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject row = data.getJSONObject(i);
                        if (!row.optString("device_identity", "").equals(currentDeviceId)) continue;
                        int cnt = row.optInt("count", 0);
                        switch (row.optString("trash_type", "").toLowerCase()) {
                            case "bio":       counts[0] += cnt; break;
                            case "nbio":      counts[1] += cnt; break;
                            case "mixed":     counts[2] += cnt; break;
                            case "hazardous": counts[3] += cnt; break;
                        }
                    }

                    todayCounts = counts;
                    todayTotal  = counts[0] + counts[1] + counts[2] + counts[3];

                    // Compute percentages for donut
                    int[] pct = new int[4];
                    if (todayTotal > 0) {
                        for (int i = 0; i < 4; i++)
                            pct[i] = Math.round((counts[i] * 100f) / todayTotal);
                    }

                    Log.d(TAG, "Today counts:" + Arrays.toString(todayCounts) + " total:" + todayTotal);

                    final int[] finalPct = pct;
                    handler.post(() -> {
                        for (DailyListener l : dailyListeners)
                            l.onDailyData(todayCounts.clone(), finalPct, todayTotal);
                    });

                } catch (Exception e) { Log.e(TAG, "Today parse error", e); }
            }
        });
    }
}