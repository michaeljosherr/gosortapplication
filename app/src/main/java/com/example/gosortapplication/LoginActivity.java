package com.example.gosortapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private GoSortApiClient apiClient;
    private ProgressBar progressBar;
    private static final String PREF_NAME = "GoSort";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_LASTNAME = "lastName";
    private static final String KEY_ROLE = "role";
    private static final String KEY_ASSIGNED_FLOOR = "assignedFloor";
    private static final String KEY_TOKEN = "token";
    // New sorter-related keys
    private static final String KEY_SORTER_DEVICE_NAME = "sorter_device_name";
    private static final String KEY_SORTER_DEVICE_ID = "sorter_device_id";
    private static final String KEY_SORTER_LOCATION = "sorter_location";
    private static final String KEY_SORTER_STATUS = "sorter_status";
    private static final String KEY_SORTER_MAINTENANCE = "sorter_maintenance";
    private static final String KEY_SORTER_FLOOR = "sorter_floor";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if already logged in
        if (getSharedPreferences(PREF_NAME, MODE_PRIVATE).getBoolean(KEY_IS_LOGGED_IN, false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        apiClient = new GoSortApiClient();
        // Get the saved IP address
        String serverIp = getSharedPreferences("GoSort", MODE_PRIVATE)
            .getString("device_ip", "");
        apiClient.setBaseUrl(serverIp);
        Log.d(TAG, "Using server IP: " + serverIp);

        EditText userNameInput = findViewById(R.id.usernameInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> {
            String userName = userNameInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (userName.isEmpty() || password.isEmpty()) {
                Log.w(TAG, "Login attempt with empty credentials");
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.i(TAG, "Attempting login for user: " + userName);
            // Show loading state
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);

            apiClient.login(userName, password, new GoSortApiClient.LoginCallback() {
                @Override
                public void onSuccess(JSONObject userData) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);

                        try {
                            Log.d(TAG, "Login successful. User data: " + userData.toString());
                            // The userData already contains the data object contents
                            JSONObject sorter = userData.optJSONObject("sorter");

                            // Start building shared preferences editor
                            android.content.SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();

                            // Save basic user data
                            editor.putString(KEY_USER_ID, userData.getString("userId"))
                                  .putString(KEY_USERNAME, userData.getString("username"))
                                  .putString(KEY_LASTNAME, userData.getString("lastName"))
                                  .putString(KEY_ROLE, userData.getString("role"))
                                  .putString(KEY_ASSIGNED_FLOOR, userData.getString("assignedFloor"))
                                  .putString(KEY_TOKEN, userData.getString("token"))
                                  .putBoolean(KEY_IS_LOGGED_IN, true);

                            // Save sorter data if available
                            if (sorter != null) {
                                editor.putString(KEY_SORTER_DEVICE_NAME, sorter.getString("device_name"))
                                      .putString(KEY_SORTER_DEVICE_ID, sorter.getString("device_identity"))
                                      .putString(KEY_SORTER_LOCATION, sorter.getString("location"))
                                      .putString(KEY_SORTER_STATUS, sorter.getString("status"))
                                      .putString(KEY_SORTER_MAINTENANCE, String.valueOf(sorter.getBoolean("maintenance_mode")))
                                      .putString(KEY_SORTER_FLOOR, sorter.getString("assigned_floor"));
                            }

                            // Apply all changes
                            editor.apply();

                            Log.i(TAG, "User preferences saved, navigating to MainActivity");
                            // Navigate to MainActivity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing login response: " + e.getMessage(), e);
                            Toast.makeText(LoginActivity.this,
                                "Error processing login response", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Login failed: " + message);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
}
