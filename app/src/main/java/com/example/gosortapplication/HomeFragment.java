package com.example.gosortapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.view.ViewTreeObserver;
import android.util.TypedValue;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // If there's a search bar inside the layout, wire a focus listener so
        // parent containers using @drawable/card_inner_bg can show the focused outline
        EditText searchBar = root.findViewById(R.id.searchBar);
        if (searchBar != null) {
            searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    // Walk up to the immediate parent CardView inner container and activate it
                    View parent = (View) v.getParent();
                    if (parent != null) {
                        parent.setActivated(hasFocus);
                    }
                }
            });
        }

        // Bind analytics values to the small progress bars on Home
        final android.widget.ProgressBar pbBiodeg = root.findViewById(R.id.progBiodeg);
        final android.widget.ProgressBar pbNonBiodeg = root.findViewById(R.id.progNonBiodeg);
        final android.widget.ProgressBar pbMixed = root.findViewById(R.id.progMixed);
        final android.widget.ProgressBar pbUnidentified = root.findViewById(R.id.progUnidentified);
        final android.widget.ProgressBar pbHazardous = root.findViewById(R.id.progHazardous);

        AnalyticsModel model = AnalyticsModel.get();
        // apply initial values
        int[] cur = model.getValues();
        if (pbBiodeg != null) pbBiodeg.setProgress(cur[0]);
        if (pbNonBiodeg != null) pbNonBiodeg.setProgress(cur[1]);
        if (pbMixed != null) pbMixed.setProgress(cur[2]);
        if (pbUnidentified != null) pbUnidentified.setProgress(cur[3]);
        if (pbHazardous != null) pbHazardous.setProgress(cur[4]);

        // listen for updates
        AnalyticsModel.Listener homeListener = new AnalyticsModel.Listener() {
            @Override
            public void onDataChanged(int[] values) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (pbBiodeg != null) pbBiodeg.setProgress(values[0]);
                        if (pbNonBiodeg != null) pbNonBiodeg.setProgress(values[1]);
                        if (pbMixed != null) pbMixed.setProgress(values[2]);
                        if (pbUnidentified != null) pbUnidentified.setProgress(values[3]);
                        if (pbHazardous != null) pbHazardous.setProgress(values[4]);
                    });
                }
            }
        };
        model.addListener(homeListener);

        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) { model.removeListener(homeListener); }
        });

        return root;
    }
}
