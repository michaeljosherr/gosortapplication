package com.example.gosortapplication;

import java.io.IOException;
import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import android.util.Log;

public class GoSortApiClient {
    private static final String TAG = "GoSortApiClient";
    private static final String API_PATH = "GoSort_Web/api/";  // Removed leading slash to match new structure
    private final OkHttpClient client;
    private String baseIp;  // Renamed from baseUrl to baseIp

    public interface ApiCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface LoginCallback {
        void onSuccess(JSONObject userData);
        void onError(String message);
    }

    public GoSortApiClient() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build();
    }

    public void setBaseUrl(String ip) {
        this.baseIp = ip;  // Store just the IP
    }

    public void testConnection(String ip, ApiCallback callback) {
        setBaseUrl(ip);
        String url = "http://" + baseIp + "/" + API_PATH + "trash_detected.php";  // Added slash after baseIp

        Request request = new Request.Builder()
            .url(url)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Silently fail like Python does
                callback.onError("");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // Just check if we got any response at all - endpoint exists
                callback.onSuccess();
                response.close();
            }
        });
    }

    public void verifyRegistration(String deviceIdentity, ApiCallback callback) {
        String url = "http://" + baseIp + "/" + API_PATH + "verify_sorter.php";  // Added slash after baseIp
        // Implementation for device registration verification
        // Will be added when implementing the registration flow
    }

    public void login(String email, String password, LoginCallback callback) {
        Log.d(TAG, "Starting login request to: " + baseIp + "/" + API_PATH + "login_api.php");  // Added slash after baseIp
        new Thread(() -> {
            try {
                URL url = new URL("http://" + baseIp + "/" + API_PATH + "login_api.php");  // Added slash after baseIp
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Create login payload - using 'email' to match API expectation
                JSONObject loginData = new JSONObject();
                loginData.put("email", email);
                loginData.put("password", password);

                Log.d(TAG, "Sending login request with payload: " + loginData.toString());

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = loginData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                StringBuilder response = new StringBuilder();

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(responseCode == 200 ? conn.getInputStream() : conn.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

                Log.d(TAG, "Login response code: " + responseCode + ", response: " + response.toString());
                JSONObject jsonResponse = new JSONObject(response.toString());

                if (jsonResponse.optBoolean("success", false)) {
                    Log.i(TAG, "Login successful");
                    // Pass the data object directly from the response
                    callback.onSuccess(jsonResponse.getJSONObject("data"));
                } else {
                    String errorMessage = jsonResponse.optString("message", "Unknown error occurred");
                    Log.w(TAG, "Login failed: " + errorMessage);
                    callback.onError(errorMessage);
                }

            } catch (Exception e) {
                Log.e(TAG, "Login request failed: " + e.getMessage(), e);
                callback.onError("Login failed: " + e.getMessage());
            }
        }).start();
    }
}