package com.example.gosortapplication;

import android.util.Log;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BinFullnessApi {
    private static final String TAG = "BinFullnessApi";
    private static final String BASE_URL = "https://gosortweb-production.up.railway.app/api/";

    public interface BinFullnessCallback {
        void onSuccess(JSONArray binData);
        void onError(String message);
    }

    public BinFullnessApi(String baseIp) {
        // baseIp is no longer used; API is hosted at a fixed URL
    }

    public void getBinFullness(String deviceIdentity, BinFullnessCallback callback) {
        new Thread(() -> {
            try {
                String urlStr = BASE_URL + "bin_fullness.php?device_identity=" + deviceIdentity;
                Log.d(TAG, "Making API request to: " + urlStr);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

                Log.d(TAG, "Raw API response: " + response.toString());
                JSONObject jsonResponse = new JSONObject(response.toString());

                if (jsonResponse.getString("status").equals("success")) {
                    JSONArray data = jsonResponse.getJSONArray("data");
                    Log.d(TAG, "Successfully parsed bin data. Number of bins: " + data.length());
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject bin = data.getJSONObject(i);
                        Log.d(TAG, String.format("Bin data [%d]: type=%s, fullness=%d%%",
                                i, bin.getString("bin_name"), bin.getInt("fullness_percentage")));
                    }
                    callback.onSuccess(data);
                } else {
                    String error = jsonResponse.optString("error", "Unknown error occurred");
                    Log.e(TAG, "API returned error status: " + error);
                    callback.onError(error);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching bin fullness: " + e.getMessage(), e);
                callback.onError("Failed to fetch bin fullness: " + e.getMessage());
            }
        }).start();
    }
}
