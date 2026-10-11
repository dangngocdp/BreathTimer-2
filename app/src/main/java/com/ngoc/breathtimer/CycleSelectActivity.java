package com.ngoc.breathtimer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class CycleSelectActivity extends Activity {

    private int minutes = 5;
    private TextView tvCycles;
    private TextView tvCycleCount;
    private int inhale, hold, exhale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycle_select);

        inhale = getIntent().getIntExtra("inhale", 4);
        hold = getIntent().getIntExtra("hold", 4);
        exhale = getIntent().getIntExtra("exhale", 4);

        TextView tvPattern = findViewById(R.id.tvPattern);
        tvPattern.setText("Nhịp thở: " + inhale + " : " + hold + " : " + exhale);

        tvCycles = findViewById(R.id.tvCycles);
        tvCycleCount = findViewById(R.id.tvCycleCount);
        updateDisplay();

        findViewById(R.id.btnMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (minutes > 5) {
                    minutes -= 5;
                    updateDisplay();
                }
            }
        });

        findViewById(R.id.btnPlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minutes += 5;
                updateDisplay();
            }
        });

        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CycleSelectActivity.this, BreathActivity.class);
                intent.putExtra("inhale", inhale);
                intent.putExtra("hold", hold);
                intent.putExtra("exhale", exhale);
                intent.putExtra("cycles", calculateCycles());
                startActivity(intent);
            }
        });
    }

    private int calculateCycles() {
        int cycleSeconds = inhale + hold + exhale;
        int totalSeconds = minutes * 60;
        // Làm tròn lên để luôn hoàn thành trọn vẹn chu kỳ cuối cùng
        return (totalSeconds + cycleSeconds - 1) / cycleSeconds;
    }

    private void updateDisplay() {
        tvCycles.setText(minutes + " phút");
        tvCycleCount.setText("≈ " + calculateCycles() + " chu kỳ thở");
    }
}
