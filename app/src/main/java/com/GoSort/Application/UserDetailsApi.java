package com.GoSort.Application;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UserDetailsApi {
    private static final String TAG = "UserDetailsApi";
    private static final String BASE_URL = "https://gosortweb-production.up.railway.app/api/";

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
