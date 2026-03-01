package com.example.gosortapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.SharedPreferences;
import android.content.Context;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final String PREF_NAME = "GoSort";
    private TextView greetingText;
    private TextView assignedAreaText;
    private Handler handler;
    private Runnable updateRunnable;
    private BinFullnessApi binFullnessApi;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            // Initialize views
            greetingText = root.findViewById(R.id.greetingText);
            assignedAreaText = root.findViewById(R.id.assignedAreaText);

            if (greetingText == null || assignedAreaText == null) {
                Log.e(TAG, "Failed to find TextView views in layout");
                return root;
            }

            // Initialize API client (hosted API uses a fixed base URL)
            binFullnessApi = new BinFullnessApi("");

            // Set default text
            greetingText.setText("Hey!");
            assignedAreaText.setText("Loading your area...");

            // Setup periodic updates
            setupPeriodicUpdates();

            // Search bar focus listener setup
            setupSearchBar(root);

            // Initialize bin status views
            initializeBinStatusViews(root);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
        }

        return root;
    }

    private void setupSearchBar(View root) {
        EditText searchBar = root.findViewById(R.id.searchBar);
        if (searchBar != null) {
            searchBar.setOnFocusChangeListener((v, hasFocus) -> {
                View parent = (View) v.getParent();
                if (parent != null) {
                    parent.setActivated(hasFocus);
                }
            });
        }
    }

    private void initializeBinStatusViews(View root) {
        // Get all bin-related views
        android.widget.ProgressBar pbBiodeg = root.findViewById(R.id.progBiodeg);
        android.widget.ProgressBar pbNonBiodeg = root.findViewById(R.id.progNonBiodeg);
        android.widget.ProgressBar pbMixed = root.findViewById(R.id.progMixed);
        android.widget.ProgressBar pbHazardous = root.findViewById(R.id.progHazardous);

        TextView txtBiodegStatus = root.findViewById(R.id.txtBiodegStatus);
        TextView txtNonBiodegStatus = root.findViewById(R.id.txtNonBiodegStatus);
        TextView txtMixedStatus = root.findViewById(R.id.txtMixedStatus);
        TextView txtHazardousStatus = root.findViewById(R.id.txtHazardousStatus);

        // Initialize with 0%
        updateBinUI(pbBiodeg, txtBiodegStatus, 0);
        updateBinUI(pbNonBiodeg, txtNonBiodegStatus, 0);
        updateBinUI(pbMixed, txtMixedStatus, 0);
        updateBinUI(pbHazardous, txtHazardousStatus, 0);

        // Fetch initial data
        updateBinFullness(pbBiodeg, pbNonBiodeg, pbMixed, pbHazardous,
                       txtBiodegStatus, txtNonBiodegStatus, txtMixedStatus,
                       txtHazardousStatus);
    }

    private void setupPeriodicUpdates() {
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUserInfo();
                updateBinFullness();
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(updateRunnable);
    }

    private void updateBinFullness() {
        if (getView() == null) return;

        // Get progress bars and status texts
        android.widget.ProgressBar pbBiodeg = getView().findViewById(R.id.progBiodeg);
        android.widget.ProgressBar pbNonBiodeg = getView().findViewById(R.id.progNonBiodeg);
        android.widget.ProgressBar pbMixed = getView().findViewById(R.id.progMixed);
        android.widget.ProgressBar pbHazardous = getView().findViewById(R.id.progHazardous);

        TextView txtBiodegStatus = getView().findViewById(R.id.txtBiodegStatus);
        TextView txtNonBiodegStatus = getView().findViewById(R.id.txtNonBiodegStatus);
        TextView txtMixedStatus = getView().findViewById(R.id.txtMixedStatus);
        TextView txtHazardousStatus = getView().findViewById(R.id.txtHazardousStatus);

        updateBinFullness(pbBiodeg, pbNonBiodeg, pbMixed, pbHazardous,
                       txtBiodegStatus, txtNonBiodegStatus, txtMixedStatus,
                       txtHazardousStatus);
    }

    private void updateBinFullness(ProgressBar pbBiodeg, ProgressBar pbNonBiodeg,
                                ProgressBar pbMixed, ProgressBar pbHazardous,
                                TextView txtBiodegStatus, TextView txtNonBiodegStatus,
                                TextView txtMixedStatus, TextView txtHazardousStatus) {

        SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String deviceId = prefs.getString("sorter_device_id", "");
        Log.d(TAG, "Retrieved device ID from preferences: " + deviceId);

        // If no device ID, try to get it from the sorter object in SharedPreferences
        if (deviceId.isEmpty()) {
            try {
                String sorterJson = prefs.getString("sorter", "");
                Log.d(TAG, "Retrieved sorter JSON: " + sorterJson);
                if (!sorterJson.isEmpty()) {
                    JSONObject sorter = new JSONObject(sorterJson);
                    deviceId = sorter.getString("device_identity");
                    Log.d(TAG, "Extracted device ID from sorter JSON: " + deviceId);
                    // Save it for future use
                    prefs.edit().putString("sorter_device_id", deviceId).apply();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting device ID from sorter data: " + e.getMessage(), e);
            }
        }

        if (deviceId.isEmpty()) {
            Log.e(TAG, "No device ID available - cannot fetch bin data");
            return;
        }

        Log.d(TAG, "Requesting bin fullness data for device: " + deviceId);
        binFullnessApi.getBinFullness(deviceId, new BinFullnessApi.BinFullnessCallback() {
            @Override
            public void onSuccess(JSONArray binData) {
                if (getActivity() == null) {
                    Log.w(TAG, "Activity is null, cannot update UI");
                    return;
                }

                getActivity().runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "Processing bin data on UI thread. Number of bins: " + binData.length());

                        // Do not reset all bins to 0 here; animate from previous value to new value
                        // updateBinUI(pbBiodeg, txtBiodegStatus, 0);
                        // updateBinUI(pbNonBiodeg, txtNonBiodegStatus, 0);
                        // updateBinUI(pbMixed, txtMixedStatus, 0);
                        // updateBinUI(pbHazardous, txtHazardousStatus, 0);

                        // Get the most recent data for each bin type
                        JSONObject latestBinData = new JSONObject();
                        for (int i = 0; i < binData.length(); i++) {
                            JSONObject bin = binData.getJSONObject(i);
                            String binName = bin.getString("bin_name").toLowerCase();
                            int fullness = bin.getInt("fullness_percentage");
                            Log.d(TAG, String.format("Processing bin: type=%s, fullness=%d%%",
                                binName, fullness));

                            // Only update if we haven't seen this bin type yet (first one is most recent)
                            if (!latestBinData.has(binName)) {
                                latestBinData.put(binName, bin);
                            }
                        }

                        // Update UI for each bin type with the correct mapping
                        updateBinTypeIfExists(latestBinData, "bio", pbBiodeg, txtBiodegStatus);
                        updateBinTypeIfExists(latestBinData, "non-bio", pbNonBiodeg, txtNonBiodegStatus);
                        updateBinTypeIfExists(latestBinData, "mixed", pbMixed, txtMixedStatus); // Changed from "recyclable" to "mixed"
                        updateBinTypeIfExists(latestBinData, "hazardous", pbHazardous, txtHazardousStatus);

                    } catch (Exception e) {
                        Log.e(TAG, "Error updating bin fullness UI: " + e.getMessage(), e);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error fetching bin fullness: " + message);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Log.d(TAG, "Setting error state for all bins");
                        updateBinUI(pbBiodeg, txtBiodegStatus, -1);
                        updateBinUI(pbNonBiodeg, txtNonBiodegStatus, -1);
                        updateBinUI(pbMixed, txtMixedStatus, -1);
                        updateBinUI(pbHazardous, txtHazardousStatus, -1);
                    });
                }
            }
        });
    }

    private void updateBinTypeIfExists(JSONObject latestBinData, String binType,
                                     ProgressBar progressBar, TextView statusText) {
        try {
            if (latestBinData.has(binType)) {
                int fullness = latestBinData.getJSONObject(binType).getInt("fullness_percentage");
                Log.d(TAG, String.format("Updating UI for %s bin: %d%%", binType, fullness));
                updateBinUI(progressBar, statusText, fullness);
            } else {
                Log.d(TAG, "No data found for bin type: " + binType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating " + binType + " bin: " + e.getMessage());
        }
    }

    private android.graphics.drawable.Drawable buildProgressDrawable(int progressColor) {
        // Background shape
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setColor(android.graphics.Color.parseColor("#DADADA"));
        bg.setCornerRadius(getResources().getDisplayMetrics().density * 8); // 8dp

        // Progress shape (solid color)
        android.graphics.drawable.GradientDrawable progShape = new android.graphics.drawable.GradientDrawable();
        progShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        progShape.setColor(progressColor);
        progShape.setCornerRadius(getResources().getDisplayMetrics().density * 8);

        // Wrap progress shape in a ClipDrawable so it clips according to level/progress
        android.graphics.drawable.ClipDrawable clip = new android.graphics.drawable.ClipDrawable(progShape, android.view.Gravity.LEFT, android.graphics.drawable.ClipDrawable.HORIZONTAL);

        // LayerDrawable: background at index 0, progress at index 1
        android.graphics.drawable.Drawable[] layers = new android.graphics.drawable.Drawable[2];
        layers[0] = bg;
        layers[1] = clip;
        android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(layers);
        // Assign ids so findDrawableByLayerId works if needed
        try {
            ld.setId(0, android.R.id.background);
            ld.setId(1, android.R.id.progress);
        } catch (Exception ignored) {}

        return ld;
    }

    private void updateBinUI(ProgressBar progressBar, TextView statusText, int fullness) {
        if (progressBar == null || statusText == null) return;

        // Ensure progress bar configured
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setSecondaryProgress(0);

        // If view hasn't been measured yet, post the update to run after layout
        if (progressBar.getWidth() == 0) {
            Log.d(TAG, "ProgressBar not laid out yet - posting update");
            progressBar.post(() -> updateBinUI(progressBar, statusText, fullness));
            return;
        }

        if (fullness == -1) {
            progressBar.setProgress(0);
            statusText.setText("Probable sensor malfunction");
            int errorColor = android.graphics.Color.parseColor("#D50000");
            statusText.setTextColor(errorColor);

            Log.d(TAG, "Setting sensor-error color on progress bar: " + errorColor);
            try {
                android.graphics.drawable.Drawable prog = buildProgressDrawable(errorColor);
                progressBar.setProgressDrawable(prog);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set programmatic progress drawable for error state", e);
            }

            progressBar.invalidate();
            progressBar.refreshDrawableState();
            return;
        }

        // Clamp fullness
        int value = Math.max(0, Math.min(100, fullness));

        Log.d(TAG, "Updating progress bar from " + progressBar.getProgress() + " to " + value);

        // Choose color by the ranges provided
        int color;
        if (value <= 25) {
            color = android.graphics.Color.parseColor("#00C853"); // Green
        } else if (value <= 50) {
            color = android.graphics.Color.parseColor("#AEEA00"); // Yellow-Green
        } else if (value <= 75) {
            color = android.graphics.Color.parseColor("#FFAB00"); // Amber
        } else if (value <= 90) {
            color = android.graphics.Color.parseColor("#FF6D00"); // Deep Orange
        } else {
            color = android.graphics.Color.parseColor("#D50000"); // Red
        }

        // Programmatically set a fresh drawable so the clip level matches progress reliably
        try {
            android.graphics.drawable.Drawable prog = buildProgressDrawable(color);
            progressBar.setProgressDrawable(prog);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set programmatic progress drawable", e);
        }

        // Set progress (animated when available)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                progressBar.setProgress(value, true);
            } else {
                int current = progressBar.getProgress();
                if (current != value) {
                    android.animation.ObjectAnimator anim = android.animation.ObjectAnimator.ofInt(progressBar, "progress", current, value);
                    anim.setDuration(500);
                    anim.setInterpolator(new android.view.animation.DecelerateInterpolator());
                    anim.start();
                }
            }
        } catch (Exception e) {
            progressBar.setProgress(value);
            Log.w(TAG, "Animation failed, set progress directly", e);
        }

        // Force redraw and state refresh
        try {
            progressBar.invalidate();
            progressBar.refreshDrawableState();
            progressBar.requestLayout();
        } catch (Exception ignored) {}

        // Extra debug: log drawable info
        try {
            android.graphics.drawable.Drawable d = progressBar.getProgressDrawable();
            Log.d(TAG, "ProgressDrawable class=" + (d != null ? d.getClass().getName() : "null") +
                    " intrinsicWidth=" + (d != null ? d.getIntrinsicWidth() : -1) +
                    " progress=" + progressBar.getProgress());
        } catch (Exception ignored) {}

        statusText.setText(value + "%");
        statusText.setTextColor(color);
    }

    private void updateUserInfo() {
        if (getActivity() == null) return;

        String username = getActivity().getSharedPreferences("GoSort", getActivity().MODE_PRIVATE)
                .getString("username", "");
        String lastName = getActivity().getSharedPreferences("GoSort", getActivity().MODE_PRIVATE)
                .getString("lastName", "");
        String assignedFloor = getActivity().getSharedPreferences("GoSort", getActivity().MODE_PRIVATE)
                .getString("assignedFloor", "");

        try {
            if (greetingText != null) {
                String displayName = lastName.isEmpty() ? username : username + " " + lastName;
                greetingText.setText(String.format("Hey, %s!", displayName));
            }

            if (assignedAreaText != null) {
                assignedAreaText.setText(String.format("Your Assigned Area/s: %s", assignedFloor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating user info in UI: " + e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
}
