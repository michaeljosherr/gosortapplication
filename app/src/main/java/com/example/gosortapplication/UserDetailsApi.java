package com.example.gosortapplication;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserDetailsApi {
    private static final String TAG = "UserDetailsApi";
    private final String baseIp;
    private static final String API_PATH = "GoSort_Web/api/";

    public interface UserDetailsCallback {
        void onSuccess(JSONObject userDetails);
        void onError(String message);
    }

    public UserDetailsApi(String baseIp) {
        this.baseIp = baseIp;
    }

    public void getUserDetails(String username, UserDetailsCallback callback) {
        new Thread(() -> {
            try {
                String urlStr = String.format("http://%s/%s/user_details_api.php?username=%s",
                        baseIp, API_PATH, username);
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

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.optBoolean("success", false)) {
                    callback.onSuccess(jsonResponse.getJSONObject("data"));
                } else {
                    String errorMessage = jsonResponse.optString("error", "Unknown error occurred");
                    callback.onError(errorMessage);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching user details: " + e.getMessage(), e);
                callback.onError("Failed to fetch user details: " + e.getMessage());
            }
        }).start();
    }
}
