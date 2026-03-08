package com.example.gosortapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class NotificationsFragment extends Fragment {
    private static final String TAG        = "NotificationsFragment";
    private static final String BASE_URL   = "https://web-production-15f71.up.railway.app/api/";
    private static final int UPDATE_INTERVAL = 5000;

    private RecyclerView rv;
    private NotificationAdapter adapter;
    private List<NotificationItem> data;
    private View layoutEmpty;
    private Handler updateHandler;
    private boolean isUpdating = false;
    private NotificationHelper notificationHelper;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUpdating && isAdded()) {
                checkBinFullness();
                updateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateHandler = new Handler(Looper.getMainLooper());
        notificationHelper = new NotificationHelper(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        isUpdating = true;
        updateHandler.post(updateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        isUpdating = false;
        updateHandler.removeCallbacks(updateRunnable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notifications, container, false);

        rv          = v.findViewById(R.id.rvNotifications);
        layoutEmpty = v.findViewById(R.id.layoutEmpty);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        NotificationRepository repo = NotificationRepository.get();
        data = repo.getAll();

        checkBinFullness();

        adapter = new NotificationAdapter(data, (item, pos) -> showActionDialog(item, pos));
        rv.setAdapter(adapter);

        // Mark all resolved
        View markAll = v.findViewById(R.id.tvMarkAll);
        if (markAll != null) {
            markAll.setOnClickListener(x -> {
                repo.markAllRead();
                adapter.notifyDataSetChanged();
                updateEmptyState();
            });
        }

        updateEmptyState();
        return v;
    }

    // ─── Action dialog (styled) ───────────────────────────────────────────────

    private void showActionDialog(NotificationItem item, int position) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_notification_action, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // View Details — opens detail fragment
        dialogView.findViewById(R.id.btnViewDetails).setOnClickListener(v -> {
            dialog.dismiss();
            NotificationRepository.get().markRead(position);
            adapter.notifyItemChanged(position);

            NotificationDetailFragment detail = NotificationDetailFragment.newInstance(
                    item.message,
                    item.meta,
                    item.isHighPriority,
                    item.binName,
                    item.fullnessLevel,
                    position
            );
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detail)
                    .addToBackStack(null)
                    .commit();
        });

        // Mark as Resolved — deletes notification
        dialogView.findViewById(R.id.btnResolved).setOnClickListener(v -> {
            dialog.dismiss();
            NotificationRepository.get().deleteNotification(position);
            adapter.notifyDataSetChanged();
            updateEmptyState();
        });

        // Cancel
        dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ─── Empty state ──────────────────────────────────────────────────────────

    private void updateEmptyState() {
        if (layoutEmpty == null) return;
        boolean empty = data.isEmpty();
        layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ─── Bin fullness polling ────────────────────────────────────────────────

    private void checkBinFullness() {
        new Thread(() -> {
            try {
                SharedPreferences prefs = requireContext()
                        .getSharedPreferences("GoSort", Context.MODE_PRIVATE);
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

                // Track latest reading per bin (API returns newest first)
                java.util.HashMap<String, JSONObject> latestPerBin = new java.util.HashMap<>();
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

                    requireActivity().runOnUiThread(() -> {
                        removeExistingNotification(binName);

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
                            data.add(0, notif);
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error checking bin fullness: " + e.getMessage(), e);
            }
        }).start();
    }

    private void removeExistingNotification(String binName) {
        for (int i = data.size() - 1; i >= 0; i--) {
            if (data.get(i).message.contains(binName)) {
                data.remove(i);
                break;
            }
        }
    }
}