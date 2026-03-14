package com.GoSort.Application;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.imageView8);
        TextView text = findViewById(R.id.textView2);

        // Create fade-in animations
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        logoFadeIn.setDuration(1000);

        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.5f, 1f);
        logoScaleX.setDuration(1000);

        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.5f, 1f);
        logoScaleY.setDuration(1000);

        ObjectAnimator textFadeIn = ObjectAnimator.ofFloat(text, "alpha", 0f, 1f);
        textFadeIn.setDuration(1000);

        // Play logo animations together
        AnimatorSet logoSet = new AnimatorSet();
        logoSet.playTogether(logoFadeIn, logoScaleX, logoScaleY);

        // Play animations sequentially
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(logoSet, textFadeIn);
        animatorSet.start();

        new Handler().postDelayed(() -> {
            // Always proceed directly to login; IP setup is no longer required
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
