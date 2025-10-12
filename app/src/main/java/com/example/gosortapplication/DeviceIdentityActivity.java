package com.example.gosortapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DeviceIdentityActivity extends AppCompatActivity {

    private View step1, step2, step3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_identity);

        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);

        Button btnAddNew = findViewById(R.id.btnAddNew);
        Button btnUseExisting = findViewById(R.id.btnUseExisting);
        Button btnSubmitNew = findViewById(R.id.btnSubmitNew);
        Button btnComplete = findViewById(R.id.btnCompleteSetup);
        Button btnBack = findViewById(R.id.btnBack);

        btnAddNew.setOnClickListener(v -> {
            step1.setVisibility(View.GONE);
            step2.setVisibility(View.VISIBLE);
        });

        btnUseExisting.setOnClickListener(v -> {
            step1.setVisibility(View.GONE);
            step3.setVisibility(View.VISIBLE);
        });

        btnSubmitNew.setOnClickListener(v -> {
            EditText et = findViewById(R.id.etNewIdentity);
            TextView tv = findViewById(R.id.tvRecognized);
            tv.setText(et.getText().toString());
            step2.setVisibility(View.GONE);
            step3.setVisibility(View.VISIBLE);
        });

        btnComplete.setOnClickListener(v -> {
            // finish setup and return to main app
            Intent i = new Intent(DeviceIdentityActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        });

        btnBack.setOnClickListener(v -> {
            if (step3.getVisibility() == View.VISIBLE) {
                step3.setVisibility(View.GONE);
                step1.setVisibility(View.VISIBLE);
            } else if (step2.getVisibility() == View.VISIBLE) {
                step2.setVisibility(View.GONE);
                step1.setVisibility(View.VISIBLE);
            } else {
                finish();
            }
        });
    }
}
