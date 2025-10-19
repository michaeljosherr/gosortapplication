package com.example.gosortapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 3000; // 3 seconds
    private GoSortApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        apiClient = new GoSortApiClient();

        new Handler().postDelayed(() -> {
            // Check if setup has been completed by looking for device_ip in preferences
            SharedPreferences prefs = getSharedPreferences("GoSort", MODE_PRIVATE);
            String deviceIp = prefs.getString("device_ip", null);

            if (deviceIp == null) {
                // Device not set up yet, go to setup
                startActivity(new Intent(SplashActivity.this, SetupActivity.class));
                finish();
                return;
            }

            // Verify if the IP is still accessible
            apiClient.testConnection(deviceIp, new GoSortApiClient.ApiCallback() {
                @Override
                public void onSuccess() {
                    // IP is still valid, proceed to login
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String message) {
                    // IP is not accessible, clear it and go to setup
                    prefs.edit().remove("device_ip").apply();
                    Intent intent = new Intent(SplashActivity.this, SetupActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }, SPLASH_DURATION);
    }
}
