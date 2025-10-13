package com.example.gosortapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {
    private GoSortApiClient apiClient;
    private EditText ipAddressInput;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        apiClient = new GoSortApiClient();
        ViewPager2 pager = findViewById(R.id.viewPager);
        Button btn = findViewById(R.id.btnSetupDevice);
        ipAddressInput = findViewById(R.id.ipAddressInput);
        progressBar = findViewById(R.id.progressBar);

        List<OnboardingAdapter.Page> pages = new ArrayList<>();
        pages.add(new OnboardingAdapter.Page("Welcome! Let's Get You Started", "Set up your device in a few easy steps."));
        pages.add(new OnboardingAdapter.Page("Connect Device", "Enter your GoSort device's IP address to connect."));
        pages.add(new OnboardingAdapter.Page("Monitor Status", "See fullness and real-time stats."));
        pages.add(new OnboardingAdapter.Page("You're Ready", "Finish setup and start sorting!"));

        OnboardingAdapter adapter = new OnboardingAdapter(pages);
        pager.setAdapter(adapter);

        View dot0 = findViewById(R.id.dot0);
        View dot1 = findViewById(R.id.dot1);
        View dot2 = findViewById(R.id.dot2);
        View dot3 = findViewById(R.id.dot3);

        View[] dots = new View[]{dot0, dot1, dot2, dot3};

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setBackgroundResource(i == position ? R.drawable.unread_dot : R.drawable.dot_unselected);
                }

                if (position == 1) { // Connect Device page
                    ipAddressInput.setVisibility(View.VISIBLE);
                    btn.setText(R.string.connect);
                } else if (position == pages.size() - 1) {
                    ipAddressInput.setVisibility(View.GONE);
                    btn.setText(R.string.get_started);
                } else {
                    ipAddressInput.setVisibility(View.GONE);
                    btn.setText(position == 0 ? R.string.setup_device : R.string.next);
                }
            }
        });

        btn.setOnClickListener(v -> {
            int pos = pager.getCurrentItem();
            if (pos == 1) { // Connect Device page
                String ipAddress = ipAddressInput.getText().toString().trim();
                if (ipAddress.isEmpty()) {
                    Toast.makeText(this, R.string.enter_ip_address, Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                btn.setEnabled(false);

                apiClient.testConnection(ipAddress, new GoSortApiClient.ApiCallback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        btn.setEnabled(true);
                        // Save IP for future use
                        getSharedPreferences("GoSort", MODE_PRIVATE)
                            .edit()
                            .putString("device_ip", ipAddress)
                            .apply();
                        pager.setCurrentItem(pos + 1, true);
                    }

                    @Override
                    public void onError(String message) {
                        progressBar.setVisibility(View.GONE);
                        btn.setEnabled(true);
                        Toast.makeText(SetupActivity.this,
                            getString(R.string.connection_failed, message),
                            Toast.LENGTH_LONG).show();
                    }
                });
            } else if (pos < pages.size() - 1) {
                pager.setCurrentItem(pos + 1, true);
            } else {
                Intent i = new Intent(SetupActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        });
    }
}
