package com.example.gosortapplication;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private ImageView imgAvatar;
    private ActivityResultLauncher<String> pickImageLauncher;

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

        // Logout -> clear activity stack and go to LoginActivity
        View btnLogout = root.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // Intent flags to clear stack
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
            btnLogout.setContentDescription(getString(R.string.action_logout));
        }

        // Accessibility: ensure avatar has content description
        if (imgAvatar != null && imgAvatar.getContentDescription() == null) {
            imgAvatar.setContentDescription(getString(R.string.profile_avatar));
        }

        return root;
    }
}
