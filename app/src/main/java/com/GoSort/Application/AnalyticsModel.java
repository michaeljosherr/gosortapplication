package com.GoSort.Application;

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

    private Call activeCall = null; // track in-flight request so we can cancel it

    public void fetch(String deviceId) {
        String newId = deviceId != null ? deviceId : "";
        if (newId.isEmpty()) { Log.e(TAG, "Empty deviceId"); return; }

        // Cancel any in-flight request for a previous device
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }

        currentDeviceId = newId;

        // Immediately clear stale data so the UI doesn't show the previous device's numbers
        todayCounts = new int[]{0, 0, 0, 0};
        todayTotal  = 0;
        handler.post(() -> {
            for (DailyListener l : dailyListeners)
                l.onDailyData(new int[]{0,0,0,0}, new int[]{0,0,0,0}, 0);
        });

        fetchTodayStats();
    }

    private void fetchTodayStats() {
        // Snapshot the device we're fetching for — guards against race conditions
        final String fetchingForDevice = currentDeviceId;

        String url = BASE_URL + "get_daily_sorting.php";
        Log.d(TAG, "Fetching today for device: " + fetchingForDevice);

        activeCall = client.newCall(new Request.Builder().url(url).build());
        activeCall.enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!call.isCanceled()) Log.e(TAG, "Today fetch failed", e);
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                // If the user already switched to another device, discard this result
                if (!fetchingForDevice.equals(currentDeviceId)) {
                    Log.d(TAG, "Discarding stale response for: " + fetchingForDevice);
                    return;
                }
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
                        // Filter by the device we requested
                        if (!row.optString("device_identity", "").equals(fetchingForDevice)) continue;
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