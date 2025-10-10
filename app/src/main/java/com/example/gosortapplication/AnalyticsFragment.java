package com.example.gosortapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AnalyticsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_analytics, container, false);

    // Use AnalyticsModel as the source of truth for the five-category values
    AnalyticsModel model = AnalyticsModel.get();
    ConcentricDonutView donut = root.findViewById(R.id.concentricDonut);
    int[] cols = new int[]{0xFFF39C12, 0xFF4A90E2, 0xFF27AE60, 0xFFF7E34A, 0xFFE74C3C};

    // helper to apply values to view + legend
    final Runnable apply = new Runnable() {
        @Override
        public void run() {
            int[] cur = model.getValues();
            if (donut != null) donut.setData(cur, cols);
            TextView tvBiodeg = root.findViewById(R.id.tvBiodeg);
            TextView tvNonBiodeg = root.findViewById(R.id.tvNonBiodeg);
            TextView tvMixed = root.findViewById(R.id.tvMixed);
            TextView tvUnidentified = root.findViewById(R.id.tvUnidentified);
            TextView tvHazardous = root.findViewById(R.id.tvHazardous);
            if (tvBiodeg != null) tvBiodeg.setText(cur[0] + "%");
            if (tvNonBiodeg != null) tvNonBiodeg.setText(cur[1] + "%");
            if (tvMixed != null) tvMixed.setText(cur[2] + "%");
            if (tvUnidentified != null) tvUnidentified.setText(cur[3] + "%");
            if (tvHazardous != null) tvHazardous.setText(cur[4] + "%");
        }
    };

    // Apply initial
    apply.run();

    // Listen for model changes while fragment is alive
    AnalyticsModel.Listener listener = new AnalyticsModel.Listener() {
        @Override
        public void onDataChanged(int[] values) {
            // post to UI thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (donut != null) donut.setData(values, cols);
                    TextView tvBiodeg = root.findViewById(R.id.tvBiodeg);
                    TextView tvNonBiodeg = root.findViewById(R.id.tvNonBiodeg);
                    TextView tvMixed = root.findViewById(R.id.tvMixed);
                    TextView tvUnidentified = root.findViewById(R.id.tvUnidentified);
                    TextView tvHazardous = root.findViewById(R.id.tvHazardous);
                    if (tvBiodeg != null) tvBiodeg.setText(values[0] + "%");
                    if (tvNonBiodeg != null) tvNonBiodeg.setText(values[1] + "%");
                    if (tvMixed != null) tvMixed.setText(values[2] + "%");
                    if (tvUnidentified != null) tvUnidentified.setText(values[3] + "%");
                    if (tvHazardous != null) tvHazardous.setText(values[4] + "%");
                });
            }
        }
    };

    model.addListener(listener);

    // Remove listener when view is destroyed to avoid leaks
    root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View v) {}

        @Override
        public void onViewDetachedFromWindow(View v) {
            model.removeListener(listener);
        }
    });

    // Demo: update the shared model after 2 seconds so both Analytics and Home update
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
        int[] newVals = new int[]{60, 30, 95, 20, 100};
        model.setValues(newVals);
    }, 2000);

        return root;
    }
}
