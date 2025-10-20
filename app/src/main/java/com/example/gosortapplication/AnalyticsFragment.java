package com.example.gosortapplication;

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

public class AnalyticsFragment extends Fragment {
    private static final String TAG = "AnalyticsFragment";
    private ConcentricDonutView donut;
    private View root;
    private final int[] cols = new int[]{0xFFF39C12, 0xFF4A90E2, 0xFF27AE60, 0xFFF7E34A, 0xFFE74C3C};

    // Store TextView references to avoid repeated findViewById calls
    private TextView tvBiodeg;
    private TextView tvNonBiodeg;
    private TextView tvMixed;
    private TextView tvUnidentified;
    private TextView tvHazardous;
    private AnalyticsModel model;
    private AnalyticsModel.Listener analyticsListener;

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
        tvUnidentified = root.findViewById(R.id.tvUnidentified);
        tvHazardous = root.findViewById(R.id.tvHazardous);
    }

    private void logValues(String prefix, int[] values) {
        if (values == null || values.length != 5) {
            Log.e(TAG, prefix + ": Invalid values array");
            return;
        }

        Log.d(TAG, prefix + ": " +
              "Bio=" + values[0] + "%, " +
              "NonBio=" + values[1] + "%, " +
              "Mixed=" + values[2] + "%, " +
              "Unidentified=" + values[3] + "%, " +
              "Hazardous=" + values[4] + "%");
    }

    private void handleError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, message);
    }

    private void updateUI(int[] values) {
        if (getActivity() == null || root == null || values == null || values.length != 5) {
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
                if (tvUnidentified != null) tvUnidentified.setText(getString(R.string.percentage_format, values[3]));
                if (tvHazardous != null) tvHazardous.setText(getString(R.string.percentage_format, values[4]));

                Log.d(TAG, "Updated percentage labels");
            } catch (Exception e) {
                Log.e(TAG, "Error updating UI: " + e.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (model != null && analyticsListener != null) {
            model.removeListener(analyticsListener);
        }
        // Clear view references
        donut = null;
        tvBiodeg = null;
        tvNonBiodeg = null;
        tvMixed = null;
        tvUnidentified = null;
        tvHazardous = null;
        root = null;
    }
}