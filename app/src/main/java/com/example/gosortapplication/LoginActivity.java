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
                            // Save user data and login status
                            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                                .edit()
                                .putBoolean("isAdmin", userData.optBoolean("isAdmin", false))
                                .putString("userName", userData.getString("username"))
                                .putString("lastName", userData.getString("lastName"))
                                .putString("token", userData.getString("token"))
                                .putBoolean(KEY_IS_LOGGED_IN, true)
                                .apply();

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
