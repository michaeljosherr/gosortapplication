package com.GoSort.Application;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import java.util.Locale;

public class AnalyticsFragment extends Fragment {
    private static final String TAG = "AnalyticsFragment";
    private ConcentricDonutView donut;
    private View root;
    // Updated to 4 colors to match the four trash categories
    private final int[] cols = new int[]{0xFFF39C12, 0xFF4A90E2, 0xFF27AE60, 0xFFE74C3C};

    // Store TextView references to avoid repeated findViewById calls
    private TextView tvBiodeg;
    private TextView tvNonBiodeg;
    private TextView tvMixed;
    private TextView tvHazardous;

    // Weekly activity bars and labels
    private View[] weekBars = new View[7];
    private TextView[] weekLabels = new TextView[7];
    private String[] weekBaseLabels = new String[7];

    private AnalyticsModel model;
    private AnalyticsModel.Listener analyticsListener;
    private AnalyticsModel.ActivityListener activityListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_analytics, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initializeViews();

        try {
            // Initialize analytics model with context
            model = AnalyticsModel.init(requireContext());

            if (model == null) {
                handleError(getString(R.string.analytics_init_failed));
                return;
            }

            // Apply initial values and log them
            int[] initialValues = model.getValues();
            logValues("Initial values", initialValues);
            updateUI(initialValues);

            // Listen for model changes
            analyticsListener = values -> {
                logValues("Received updated values", values);
                updateUI(values);
            };
            model.addListener(analyticsListener);

            // Listen for weekly activity changes
            activityListener = counts -> {
                Log.d(TAG, "Received weekly activity: " + java.util.Arrays.toString(counts));
                updateWeeklyUI(counts);
            };
            model.addActivityListener(activityListener);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing analytics: " + e.getMessage());
            handleError(getString(R.string.analytics_load_failed));
        }
    }

    private void initializeViews() {
        donut = root.findViewById(R.id.concentricDonut);
        tvBiodeg = root.findViewById(R.id.tvBiodeg);
        tvNonBiodeg = root.findViewById(R.id.tvNonBiodeg);
        tvMixed = root.findViewById(R.id.tvMixed);
        tvHazardous = root.findViewById(R.id.tvHazardous);

        // Weekly bars
        weekBars[0] = root.findViewById(R.id.bar_day0);
        weekBars[1] = root.findViewById(R.id.bar_day1);
        weekBars[2] = root.findViewById(R.id.bar_day2);
        weekBars[3] = root.findViewById(R.id.bar_day3);
        weekBars[4] = root.findViewById(R.id.bar_day4);
        weekBars[5] = root.findViewById(R.id.bar_day5);
        weekBars[6] = root.findViewById(R.id.bar_day6);

        weekLabels[0] = root.findViewById(R.id.label_day0);
        weekLabels[1] = root.findViewById(R.id.label_day1);
        weekLabels[2] = root.findViewById(R.id.label_day2);
        weekLabels[3] = root.findViewById(R.id.label_day3);
        weekLabels[4] = root.findViewById(R.id.label_day4);
        weekLabels[5] = root.findViewById(R.id.label_day5);
        weekLabels[6] = root.findViewById(R.id.label_day6);

        // capture base labels so we don't keep appending counts repeatedly
        for (int i = 0; i < 7; i++) {
            TextView lbl = weekLabels[i];
            weekBaseLabels[i] = (lbl != null) ? lbl.getText().toString() : "";
        }
    }

    private void logValues(String prefix, int[] values) {
        if (values == null || values.length != 4) {
            Log.e(TAG, prefix + ": Invalid values array (expected 4)");
            return;
        }

        Log.d(TAG, prefix + ": " +
              "Bio=" + values[0] + "%, " +
              "NonBio=" + values[1] + "%, " +
              "Mixed=" + values[2] + "%, " +
              "Hazardous=" + values[3] + "%");
    }

    private void handleError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, message);
    }

    private void updateUI(int[] values) {
        if (getActivity() == null || root == null || values == null || values.length != 4) {
            Log.e(TAG, "Invalid state in updateUI");
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                if (donut != null) {
                    donut.setData(values, cols);
                    Log.d(TAG, "Updated donut chart with values");
                }

                if (tvBiodeg != null) tvBiodeg.setText(getString(R.string.percentage_format, values[0]));
                if (tvNonBiodeg != null) tvNonBiodeg.setText(getString(R.string.percentage_format, values[1]));
                if (tvMixed != null) tvMixed.setText(getString(R.string.percentage_format, values[2]));
                if (tvHazardous != null) tvHazardous.setText(getString(R.string.percentage_format, values[3]));

                Log.d(TAG, "Updated percentage labels");
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI: " + e.getMessage());
            }
        });
    }

    // Update weekly activity UI - counts length expected to be 7 (day0 = 6 days ago .. day6 = today)
    private void updateWeeklyUI(int[] counts) {
        if (getActivity() == null || root == null || counts == null || counts.length != 7) {
            Log.e(TAG, "Invalid state in updateWeeklyUI");
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                int max = 0;
                for (int c : counts) if (c > max) max = c;

                // Convert dp to px
                float density = getResources().getDisplayMetrics().density;
                int maxPx = (int) (120 * density); // max bar height
                int minPx = (int) (8 * density);   // min bar height so it's always visible

                for (int i = 0; i < 7; i++) {
                    View bar = weekBars[i];
                    if (bar == null) continue;
                    int height = minPx;
                    if (max > 0) {
                        height = minPx + Math.round(((float) counts[i] / (float) max) * (maxPx - minPx));
                    } else {
                        // all counts are zero - keep minimal height
                        height = minPx;
                    }
                    android.view.ViewGroup.LayoutParams lp = bar.getLayoutParams();
                    lp.height = Math.max(1, height);
                    bar.setLayoutParams(lp);

                    // Update the label to show base label + count (e.g., Mon\n3)
                    TextView lbl = weekLabels[i];
                    if (lbl != null) {
                        String base = weekBaseLabels[i] != null ? weekBaseLabels[i] : "";
                        lbl.setText(String.format(Locale.getDefault(), "%s\n%d", base, counts[i]));
                    }
                }

                Log.d(TAG, "Weekly UI updated");
            } catch (Exception e) {
                Log.e(TAG, "Error updating weekly UI: " + e.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (model != null && analyticsListener != null) {
            model.removeListener(analyticsListener);
        }
        if (model != null && activityListener != null) {
            model.removeActivityListener(activityListener);
        }
        // Clear view references
        donut = null;
        tvBiodeg = null;
        tvNonBiodeg = null;
        tvMixed = null;
        tvHazardous = null;
        weekBars = null;
        weekLabels = null;
        weekBaseLabels = null;
        root = null;
    }
}
