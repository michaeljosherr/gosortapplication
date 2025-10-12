package com.example.gosortapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ViewAnimator;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        ViewPager2 pager = findViewById(R.id.viewPager);
        Button btn = findViewById(R.id.btnSetupDevice);

        List<OnboardingAdapter.Page> pages = new ArrayList<>();
        pages.add(new OnboardingAdapter.Page("Welcome! Let's Get You Started", "Set up your device in a few easy steps."));
        pages.add(new OnboardingAdapter.Page("Connect Device", "Discover and connect to your sorter device."));
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
                // If on first page, show 'SET UP DEVICE' as primary action; otherwise show NEXT
                if (position == 0) {
                    btn.setText("SET UP DEVICE");
                } else if (position == pages.size() - 1) {
                    btn.setText("GET STARTED");
                } else {
                    btn.setText("NEXT");
                }
            }
        });

        btn.setOnClickListener(v -> {
            int pos = pager.getCurrentItem();
            if (pos == 0) {
                // Open the IP scan/setup flow
                Intent i = new Intent(SetupActivity.this, SetupScanActivity.class);
                startActivity(i);
                return;
            }

            if (pos < pages.size() - 1) {
                pager.setCurrentItem(pos + 1, true);
            } else {
                Intent i = new Intent(SetupActivity.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        });
    }
}
