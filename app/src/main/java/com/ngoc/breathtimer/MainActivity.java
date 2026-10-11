package com.ngoc.breathtimer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Android 13 trở lên bắt buộc phải xin quyền hiển thị thông báo
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        setupPattern(R.id.btn444, 4, 4, 4);
        setupPattern(R.id.btn448, 4, 4, 8);
        setupPattern(R.id.btn478, 4, 7, 8);
        setupPattern(R.id.btn4168, 4, 16, 8);
    }

    private void setupPattern(int buttonId, final int inhale, final int hold, final int exhale) {
        Button btn = findViewById(buttonId);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CycleSelectActivity.class);
                intent.putExtra("inhale", inhale);
                intent.putExtra("hold", hold);
                intent.putExtra("exhale", exhale);
                startActivity(intent);
            }
        });
    }
}
