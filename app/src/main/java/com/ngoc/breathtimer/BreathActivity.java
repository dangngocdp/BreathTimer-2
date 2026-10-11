package com.ngoc.breathtimer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class BreathActivity extends Activity implements BreathService.TimerCallback {

    private TextView tvPhase, tvCount, tvCycle;
    private Button btnStop;
    private BreathService service;
    private boolean bound = false;
    private int inhale, hold, exhale, cycles;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            BreathService.LocalBinder localBinder = (BreathService.LocalBinder) binder;
            service = localBinder.getService();
            service.setCallback(BreathActivity.this);
            bound = true;
            if (service.isRunning()) {
                onTick(service.getCurrentPhaseName(), service.getRemaining(),
                        service.getCurrentCycle(), service.getTotalCycles());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breath);

        tvPhase = findViewById(R.id.tvPhase);
        tvCount = findViewById(R.id.tvCount);
        tvCycle = findViewById(R.id.tvCycle);
        btnStop = findViewById(R.id.btnStop);

        inhale = getIntent().getIntExtra("inhale", 4);
        hold = getIntent().getIntExtra("hold", 4);
        exhale = getIntent().getIntExtra("exhale", 4);
        cycles = getIntent().getIntExtra("cycles", 10);

        Intent serviceIntent = new Intent(this, BreathService.class);
        serviceIntent.putExtra("inhale", inhale);
        serviceIntent.putExtra("hold", hold);
        serviceIntent.putExtra("exhale", exhale);
        serviceIntent.putExtra("cycles", cycles);
        startForegroundService(serviceIntent);

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bound && service != null) {
                    service.stopSession();
                }
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent serviceIntent = new Intent(this, BreathService.class);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            if (service != null) {
                service.setCallback(null);
            }
            unbindService(connection);
            bound = false;
        }
    }

    @Override
    public void onTick(final String phaseName, final int remainingSeconds,
                        final int currentCycle, final int totalCycles) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvPhase.setText(phaseName);
                tvCount.setText(String.valueOf(remainingSeconds));
                tvCycle.setText("Chu kỳ " + currentCycle + "/" + totalCycles);
            }
        });
    }

    @Override
    public void onFinished() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvPhase.setText("Hoàn thành!");
                tvCount.setText("");
                btnStop.setText("Xong");
            }
        });
    }
}
