package com.GoSort.Application;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SetupScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_scan);

        LinearLayout list = findViewById(R.id.listContainer);
        // sample IPs
        String[] ips = new String[]{"192.168.0.101", "192.168.0.102", "192.168.0.103"};
        for (String ip : ips) {
            TextView tv = new TextView(this);
            tv.setText(ip);
            tv.setTextSize(16f);
            tv.setPadding(12,12,12,12);
            tv.setBackgroundResource(R.drawable.card_inner_bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,8,0,8);
            list.addView(tv, lp);

            tv.setOnClickListener(v -> {
                // simulate choosing IP -> go to device identity configuration (placeholder)
                Intent i = new Intent(SetupScanActivity.this, DeviceIdentityActivity.class);
                i.putExtra("ip", ip);
                startActivity(i);
                finish();
            });
        }

        Button choose = findViewById(R.id.btnChooseIp);
        choose.setOnClickListener(v -> {
            Intent i = new Intent(SetupScanActivity.this, DeviceIdentityActivity.class);
            startActivity(i);
            finish();
        });
    }
}
