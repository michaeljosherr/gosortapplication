package com.example.gosortapplication;

import android.os.Bundle;
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

    // Data for 5 bin categories (values are example percentages or counts)
    int biodegradable = 70; // percent
    int nonBiodegradable = 45;
    int mixed = 85;
    int unidentified = 30;
    int hazardous = 5;

    ConcentricDonutView donut = root.findViewById(R.id.concentricDonut);
    int[] vals = new int[]{biodegradable, nonBiodegradable, mixed, unidentified, hazardous};
    int[] cols = new int[]{0xFFF39C12, 0xFF4A90E2, 0xFF27AE60, 0xFFF7E34A, 0xFFE74C3C};
    donut.setData(vals, cols);

    TextView tvBiodeg = root.findViewById(R.id.tvBiodeg);
    TextView tvNonBiodeg = root.findViewById(R.id.tvNonBiodeg);
    TextView tvMixed = root.findViewById(R.id.tvMixed);
    TextView tvUnidentified = root.findViewById(R.id.tvUnidentified);
    TextView tvHazardous = root.findViewById(R.id.tvHazardous);

    if (tvBiodeg != null) tvBiodeg.setText(biodegradable + "%");
    if (tvNonBiodeg != null) tvNonBiodeg.setText(nonBiodegradable + "%");
    if (tvMixed != null) tvMixed.setText(mixed + "%");
    if (tvUnidentified != null) tvUnidentified.setText(unidentified + "%");
    if (tvHazardous != null) tvHazardous.setText(hazardous + "%");

        return root;
    }
}
