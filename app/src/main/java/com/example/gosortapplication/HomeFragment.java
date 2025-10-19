package com.example.gosortapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import org.json.JSONObject;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private TextView greetingText;
    private TextView assignedAreaText;
    private Handler handler;
    private Runnable updateRunnable;

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

            // Set default text
            greetingText.setText("Hey!");
            assignedAreaText.setText("Loading your area...");

            // Setup periodic updates
            setupPeriodicUpdates();

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

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
        }

        return root;
    }

    private void setupPeriodicUpdates() {
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUserInfo();
                handler.postDelayed(this, 2000); // 2 seconds interval
            }
        };
        handler.post(updateRunnable);
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
