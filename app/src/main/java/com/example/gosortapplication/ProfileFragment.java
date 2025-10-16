package com.example.gosortapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class ProfileFragment extends Fragment {

    private ImageView imgAvatar;
    private ActivityResultLauncher<String> pickImageLauncher;
    private static final String PREF_NAME = "GoSort";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_DEVICE_IP = "device_ip";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        imgAvatar = root.findViewById(R.id.imgAvatar);

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
}
