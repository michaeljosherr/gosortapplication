package com.example.gosortapplication;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserDetailsApi {
    private static final String TAG = "UserDetailsApi";
    private static final String BASE_URL = "https://web-production-15f71.up.railway.app/api/";

    public interface UserDetailsCallback {
        void onSuccess(JSONObject userDetails);
        void onError(String message);
    }

    public UserDetailsApi(String baseIp) {
        // baseIp is no longer used; API is hosted at a fixed URL
    }

    public void getUserDetails(String username, UserDetailsCallback callback) {
        new Thread(() -> {
            try {
                String urlStr = BASE_URL + "user_details_api.php?username=" + username;
                Log.d(TAG, "Making API request to: " + urlStr);

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode == 200
                                ? conn.getInputStream()
                                : conn.getErrorStream()));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);
                br.close();

                Log.d(TAG, "Raw API response: " + response.toString());

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.optBoolean("success", false)) {
                    callback.onSuccess(jsonResponse.getJSONObject("data"));
                } else {
                    String errorMessage = jsonResponse.optString("error", "Unknown error occurred");
                    Log.e(TAG, "API returned error: " + errorMessage);
                    callback.onError(errorMessage);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching user details: " + e.getMessage(), e);
                callback.onError("Failed to fetch user details: " + e.getMessage());
            }
        }).start();
    }
}