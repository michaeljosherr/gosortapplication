package com.example.gosortapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG                      = "MainActivity";
    private static final int    NOTIFICATION_PERMISSION_CODE = 123;
    private static final String BASE_URL                 = "https://web-production-15f71.up.railway.app/api/";
    private static final int    UPDATE_INTERVAL          = 5000;

    // Shared polling state — lives here so it survives fragment switches
    private final Map<String, Integer> lastNotifiedFullness = new HashMap<>();
    private Handler pollingHandler;
    private NotificationHelper notificationHelper;
    private boolean isPolling = false;

    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPolling) {
                checkBinFullness();
                pollingHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Check if user is logged in, redirect to login if not
        if (!getSharedPreferences("GoSort", MODE_PRIVATE).getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Apply insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
            return WindowInsetsCompat.CONSUMED;
        });

        // Initialize repository and polling helpers
        NotificationRepository.get().init(this);
        notificationHelper = new NotificationHelper(this);
        pollingHandler = new Handler(Looper.getMainLooper());

        // Observe unread count → update badge from anywhere
        NotificationRepository.get().addListener(newCount -> runOnUiThread(() -> {
            if (newCount <= 0) {
                if (bottomNavigation.getBadge(R.id.nav_notifications) != null)
                    bottomNavigation.removeBadge(R.id.nav_notifications);
            } else {
                bottomNavigation.getOrCreateBadge(R.id.nav_notifications).setNumber(newCount);
            }
        }));

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_analytics) {
                selectedFragment = new AnalyticsFragment();
            } else if (itemId == R.id.nav_notifications) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPolling = true;
        pollingHandler.post(pollingRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPolling = false;
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    // ─── Bin fullness polling ─────────────────────────────────────────────────

    private void checkBinFullness() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = getSharedPreferences("GoSort", Context.MODE_PRIVATE);
                String deviceId = prefs.getString("sorter_device_id", "");

                if (deviceId.isEmpty()) {
                    Log.e(TAG, "Device ID not set in preferences");
                    return;
                }

                URL url = new URL(BASE_URL + "bin_fullness.php?device_identity=" + deviceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return;

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                if (!json.getString("status").equals("success")) return;

                JSONArray readings = json.getJSONArray("data");

                // Keep only the latest reading per bin (API returns newest first)
                HashMap<String, JSONObject> latestPerBin = new HashMap<>();
                for (int i = 0; i < readings.length(); i++) {
                    JSONObject reading = readings.getJSONObject(i);
                    String binName = reading.getString("bin_name");
                    if (!latestPerBin.containsKey(binName))
                        latestPerBin.put(binName, reading);
                }

                for (JSONObject latest : latestPerBin.values()) {
                    String binName  = latest.getString("bin_name");
                    int    fullness = latest.getInt("fullness_percentage");
                    String timestamp= latest.getString("timestamp");

                    runOnUiThread(() -> {
                        // Skip if fullness hasn't changed since last notification
                        Integer prev = lastNotifiedFullness.get(binName);
                        if (prev != null && prev == fullness) return;

                        NotificationItem notif = null;
                        String metaBase = String.format("© %s | Device: %s | Bin: %s",
                                timestamp, deviceId, binName);

                        if (fullness == -1) {
                            notif = new NotificationItem(
                                    "Bin '" + binName + "' sensor MALFUNCTION detected! Please check the sensor immediately.",
                                    metaBase + " | Status: Sensor Error",
                                    true, binName, -1);
                            notificationHelper.showBinAlert("Bin Sensor Malfunction",
                                    "Bin '" + binName + "' sensor is not responding properly.");

                        } else if (fullness >= 100) {
                            notif = new NotificationItem(
                                    "Bin '" + binName + "' is COMPLETELY FULL at 100%! Immediate emptying required.",
                                    metaBase + " | Fullness: 100% | Priority: CRITICAL",
                                    true, binName, fullness);
                            notificationHelper.showBinAlert("Bin Completely Full - CRITICAL",
                                    "Bin '" + binName + "' has reached 100% capacity. Empty immediately!");

                        } else if (fullness >= 90) {
                            notif = new NotificationItem(
                                    "Bin '" + binName + "' is FULL at " + fullness + "%. Please empty immediately.",
                                    metaBase + " | Fullness: " + fullness + "% | Priority: HIGH",
                                    true, binName, fullness);
                            notificationHelper.showBinAlert("Bin Full Alert",
                                    "Bin '" + binName + "' is " + fullness + "% full. Please empty!");

                        } else if (fullness >= 50) {
                            notif = new NotificationItem(
                                    "Bin '" + binName + "' has reached " + fullness + "% capacity. Consider emptying soon.",
                                    metaBase + " | Fullness: " + fullness + "% | Priority: MEDIUM",
                                    false, binName, fullness);
                            notificationHelper.showBinAlert("Bin Half Full Notice",
                                    "Bin '" + binName + "' is now " + fullness + "% full.");
                        }

                        if (notif != null) {
                            // Remove stale notification for this bin then insert the fresh one
                            removeExistingNotification(binName);
                            lastNotifiedFullness.put(binName, fullness);
                            NotificationRepository.get().getAll().add(0, notif);
                            // Repository handles persistence + badge update via listeners
                            NotificationRepository.get().notifyListenersPublic();
                            // If NotificationsFragment is currently visible, tell it to refresh
                            Fragment current = getSupportFragmentManager()
                                    .findFragmentById(R.id.fragment_container);
                            if (current instanceof NotificationsFragment) {
                                ((NotificationsFragment) current).onNewNotification();
                            }
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error checking bin fullness: " + e.getMessage(), e);
            }
        }).start();
    }

    private void removeExistingNotification(String binName) {
        java.util.List<NotificationItem> data = NotificationRepository.get().getAll();
        for (int i = data.size() - 1; i >= 0; i--) {
            if (data.get(i).message.contains(binName)) {
                data.remove(i);
                break;
            }
        }
    }

    // ─── Allow NotificationsFragment to clear a resolved bin's cached fullness ─

    public void clearLastNotifiedFullness(String binName) {
        lastNotifiedFullness.remove(binName);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission is required for bin alerts",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}