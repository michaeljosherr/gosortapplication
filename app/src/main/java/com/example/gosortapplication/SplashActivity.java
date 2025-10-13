package com.example.gosortapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Check if setup has been completed by looking for device_ip in preferences
                SharedPreferences prefs = getSharedPreferences("GoSort", MODE_PRIVATE);
                String deviceIp = prefs.getString("device_ip", null);

                Intent intent;
                if (deviceIp == null) {
                    // Device not set up yet, go to setup
                    intent = new Intent(SplashActivity.this, SetupActivity.class);
                } else {
                    // Device is set up, proceed to login
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }
}
