package com.example.gosortapplication;

import android.os.Handler;
import android.os.Looper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class GoSortApiClient {
    private final OkHttpClient client;
    private final Handler mainHandler;
    private String baseUrl;

    public interface ApiCallback {
        void onSuccess();
        void onError(String message);
    }

    public GoSortApiClient() {
        this.client = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setBaseUrl(String ip) {
        this.baseUrl = "http://" + ip + "/GoSort_Web/gs_DB";
    }

    public void testConnection(String ip, ApiCallback callback) {
        setBaseUrl(ip);
        String url = baseUrl + "/trash_detected.php";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Connection failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    mainHandler.post(() -> callback.onError("Server returned error: " + response.code()));
                }
                response.close();
            }
        });
    }
}
