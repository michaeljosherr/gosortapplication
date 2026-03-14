package com.GoSort.Application;

import android.Manifest;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG                      = "MainActivity";
    private static final int    NOTIFICATION_PERMISSION_CODE = 123;
    private static final String BASE_URL                 = "https://web-production-15f71.up.railway.app/api/";
    private static final int    UPDATE_INTERVAL          = 5000;

    // All assigned device IDs for this user — populated on start
    private final List<String> assignedDeviceIds = new ArrayList<>();

    // Tracks last-notified fullness per "deviceId|binName" key to avoid
    // duplicate notifications and push-alert spam across all devices
    private final Map<String, Integer> lastNotifiedFullness = new HashMap<>();

    private Handler pollingHandler;
    private NotificationHelper notificationHelper;
    private boolean isPolling = false;

    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPolling) {
                // Poll every assigned device on each tick
                synchronized (assignedDeviceIds) {
                    for (String deviceId : assignedDeviceIds) {
                        checkBinFullnessForDevice(deviceId);
                    }
                }
                pollingHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        if (!getSharedPreferences("GoSort", MODE_PRIVATE).getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) ->
                WindowInsetsCompat.CONSUMED);

        // Initialize repository and polling helpers
        NotificationRepository.get().init(this);
        notificationHelper = new NotificationHelper(this);
        pollingHandler = new Handler(Looper.getMainLooper());

        // Observe unread count → update badge from any fragment
        NotificationRepository.get().addListener(newCount -> runOnUiThread(() -> {
            if (newCount <= 0) {
                if (bottomNavigation.getBadge(R.id.nav_notifications) != null)
                    bottomNavigation.removeBadge(R.id.nav_notifications);
            } else {
                bottomNavigation.getOrCreateBadge(R.id.nav_notifications).setNumber(newCount);
            }
        }));

        // Load default fragment
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

        // Fetch all assigned devices for this user, then start polling
        fetchAssignedDevices();
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

    // ─── Fetch all assigned devices for this user ─────────────────────────────

    private void fetchAssignedDevices() {
        SharedPreferences prefs = getSharedPreferences("GoSort", MODE_PRIVATE);
        String username = prefs.getString("username", "");

        if (username.isEmpty()) {
            // Fallback: use the single device saved at login if username missing
            String fallback = prefs.getString("sorter_device_id", "");
            if (!fallback.isEmpty()) {
                synchronized (assignedDeviceIds) {
                    assignedDeviceIds.add(fallback);
                }
                Log.w(TAG, "No username found, falling back to single device: " + fallback);
            }
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "user_details_api.php?username=" + username);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader br = new BufferedReader(new InputStreamReader(
                        conn.getResponseCode() == 200
                                ? conn.getInputStream()
                                : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject json = new JSONObject(sb.toString());
                if (!json.optBoolean("success", false)) {
                    Log.e(TAG, "fetchAssignedDevices: API returned failure");
                    return;
                }

                JSONArray sorters = json.getJSONObject("data").getJSONArray("assigned_sorters");

                synchronized (assignedDeviceIds) {
                    assignedDeviceIds.clear();
                    for (int i = 0; i < sorters.length(); i++) {
                        String id = sorters.getJSONObject(i).optString("device_identity", "");
                        if (!id.isEmpty()) {
                            assignedDeviceIds.add(id);
                            Log.d(TAG, "Assigned device: " + id);
                        }
                    }
                }

                Log.d(TAG, "Total assigned devices: " + assignedDeviceIds.size());

            } catch (Exception e) {
                Log.e(TAG, "fetchAssignedDevices error: " + e.getMessage());

                // Fallback to single saved device if fetch fails
                String fallback = getSharedPreferences("GoSort", MODE_PRIVATE)
                        .getString("sorter_device_id", "");
                if (!fallback.isEmpty()) {
                    synchronized (assignedDeviceIds) {
                        if (!assignedDeviceIds.contains(fallback))
                            assignedDeviceIds.add(fallback);
                    }
                }
            }
        }).start();
    }

    // ─── Bin fullness polling — one call per assigned device ─────────────────

    private void checkBinFullnessForDevice(String deviceId) {
        new Thread(() -> {
            try {
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

                // Keep only the latest reading per bin
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
                        // Use "deviceId|binName" as the dedup key so bins with the
                        // same name on different devices don't interfere with each other
                        String dedupKey = deviceId + "|" + binName;
                        Integer prev = lastNotifiedFullness.get(dedupKey);
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
                            removeExistingNotification(deviceId, binName);
                            lastNotifiedFullness.put(dedupKey, fullness);
                            NotificationRepository.get().getAll().add(0, notif);
                            NotificationRepository.get().notifyListenersPublic();
                            Fragment current = getSupportFragmentManager()
                                    .findFragmentById(R.id.fragment_container);
                            if (current instanceof NotificationsFragment)
                                ((NotificationsFragment) current).onNewNotification();
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "checkBinFullness error for device " + deviceId + ": " + e.getMessage());
            }
        }).start();
    }

    private void removeExistingNotification(String deviceId, String binName) {
        java.util.List<NotificationItem> data = NotificationRepository.get().getAll();
        // Match on both device ID and bin name to avoid removing notifs from other devices
        for (int i = data.size() - 1; i >= 0; i--) {
            NotificationItem item = data.get(i);
            if (item.message.contains(binName) && item.meta.contains(deviceId)) {
                data.remove(i);
                break;
            }
        }
    }

    // ─── Called by NotificationsFragment when a notification is resolved ──────

    public void clearLastNotifiedFullness(String deviceId, String binName) {
        lastNotifiedFullness.remove(deviceId + "|" + binName);
    }

    // Keep old single-arg version for backward compatibility
    public void clearLastNotifiedFullness(String binName) {
        // Remove all keys that end with this bin name across all devices
        lastNotifiedFullness.entrySet().removeIf(e -> e.getKey().endsWith("|" + binName));
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