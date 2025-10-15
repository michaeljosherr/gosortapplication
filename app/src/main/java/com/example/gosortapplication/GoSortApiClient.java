package com.example.gosortapplication;

import java.io.IOException;
import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

public class GoSortApiClient {
    private final OkHttpClient client;
    private String baseIp;  // Renamed from baseUrl to baseIp

    public interface ApiCallback {
        void onSuccess();
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
        String url = "http://" + baseIp + "/GoSort_Web/gs_DB/trash_detected.php";

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
        String url = "http://" + baseIp + "/GoSort_Web/gs_DB/verify_sorter.php";
        // Implementation for device registration verification
        // Will be added when implementing the registration flow
    }
}