package com.example.gosortapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import org.json.JSONArray;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private ImageView imgAvatar;
    private ActivityResultLauncher<String> pickImageLauncher;
    private static final String PREF_NAME = "GoSort";
    private static final String KEY_DEVICE_IP = "device_ip";

    // UI elements for user details
    private TextView txtUsername;
    private TextView txtRole;
    private TextView txtEmail;
    private TextView txtAssignedFloor;
    private TextView txtSorterLocation;
    private LinearLayout assignedSortersContainer;
    private Handler handler;
    private Runnable updateRunnable;
    private UserDetailsApi userDetailsApi;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        imgAvatar = root.findViewById(R.id.imgAvatar);
        txtUsername = root.findViewById(R.id.txtUsername);
        txtRole = root.findViewById(R.id.txtRole);
        txtEmail = root.findViewById(R.id.txtEmail);
        txtAssignedFloor = root.findViewById(R.id.txtAssignedFloor);
        txtSorterLocation = root.findViewById(R.id.txtSorterLocation);
        assignedSortersContainer = root.findViewById(R.id.assignedSortersContainer);

        // Initialize API client
        String serverIp = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DEVICE_IP, "");
        userDetailsApi = new UserDetailsApi(serverIp);

        // Setup periodic updates
        setupPeriodicUpdates();

        // Register image picker launcher
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
            if (result != null) {
                imgAvatar.setImageURI(result);
            }
        });

        // Edit profile -> pick an image
        View btnEdit = root.findViewById(R.id.btnEditProfile);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
            btnEdit.setContentDescription(getString(R.string.action_edit_profile));
        }

        // Logout -> clear user data but preserve IP
        MaterialButton logoutButton = root.findViewById(R.id.btnLogout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                // Get the current IP before clearing
                SharedPreferences prefs = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                String savedIp = prefs.getString(KEY_DEVICE_IP, "");

                // Clear preferences but preserve IP
                prefs.edit()
                    .clear()
                    .putString(KEY_DEVICE_IP, savedIp)
                    .apply();

                // Navigate to LoginActivity
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
            logoutButton.setContentDescription(getString(R.string.action_logout));
        }

        // Accessibility: ensure avatar has content description
        if (imgAvatar != null && imgAvatar.getContentDescription() == null) {
            imgAvatar.setContentDescription(getString(R.string.profile_avatar));
        }

        return root;
    }

    private void setupPeriodicUpdates() {
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                fetchUserDetails();
                handler.postDelayed(this, 2000); // 2 seconds interval
            }
        };
        handler.post(updateRunnable);
    }

    private void fetchUserDetails() {
        if (getActivity() == null) return;

        String username = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString("username", "");

        userDetailsApi.getUserDetails(username, new UserDetailsApi.UserDetailsCallback() {
            @Override
            public void onSuccess(JSONObject userDetails) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> updateUI(userDetails));
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error fetching user details: " + message);
            }
        });
    }

    private void updateUI(JSONObject userDetails) {
        try {
            JSONObject userInfo = userDetails.getJSONObject("user_info");
            JSONArray sorters = userDetails.getJSONArray("assigned_sorters");

            // Update basic info
            txtUsername.setText(getString(R.string.full_name_format,
                userInfo.getString("userName"),
                userInfo.getString("lastName")));
            txtRole.setText(userInfo.getString("role"));
            txtEmail.setText(getString(R.string.email_format, userInfo.getString("email")));
            txtAssignedFloor.setText(getString(R.string.assigned_floor_format,
                userInfo.getString("assigned_floor")));

            // Update sorter location if available
            if (sorters.length() > 0) {
                JSONObject firstSorter = sorters.getJSONObject(0);
                txtSorterLocation.setText(getString(R.string.location_format,
                    firstSorter.getString("location")));
                txtSorterLocation.setVisibility(View.VISIBLE);
            } else {
                txtSorterLocation.setVisibility(View.GONE);
            }

            // Update assigned sorters
            updateAssignedSorters(sorters);

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage());
        }
    }

    private void updateAssignedSorters(JSONArray sorters) {
        assignedSortersContainer.removeAllViews();
        try {
            for (int i = 0; i < sorters.length(); i++) {
                JSONObject sorter = sorters.getJSONObject(i);
                View sorterView = getLayoutInflater().inflate(R.layout.item_assigned_sorter,
                        assignedSortersContainer, false);

                TextView txtDeviceName = sorterView.findViewById(R.id.txtDeviceName);
                TextView txtLocation = sorterView.findViewById(R.id.txtLocation);
                TextView txtStatus = sorterView.findViewById(R.id.txtStatus);

                txtDeviceName.setText(sorter.getString("device_name"));
                txtLocation.setText(sorter.getString("location"));
                txtStatus.setText(sorter.getString("status"));

                assignedSortersContainer.addView(sorterView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating sorters: " + e.getMessage());
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
