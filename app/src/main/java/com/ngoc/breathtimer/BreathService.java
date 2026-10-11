package com.ngoc.breathtimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class BreathService extends Service {

    public static final String CHANNEL_ID = "breath_running";
    private static final int NOTIF_ID = 1;

    public interface TimerCallback {
        void onTick(String phaseName, int remainingSeconds, int currentCycle, int totalCycles);
        void onFinished();
    }

    private final IBinder binder = new LocalBinder();
    private TimerCallback callback;

    private int inhale, hold, exhale, totalCycles;
    private int currentCycle = 1;
    private int phaseIndex = 0; // 0 = Hít vào, 1 = Giữ, 2 = Thở ra
    private int remaining;
    private boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Vibrator vibrator;
    private ToneGenerator toneGen;
    private PowerManager.WakeLock wakeLock;

    private final String[] phaseNames = {"Hít vào", "Giữ", "Thở ra"};

    public class LocalBinder extends Binder {
        BreathService getService() {
            return BreathService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);
        } catch (Exception e) {
            toneGen = null;
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BreathTimer::TimerWakeLock");
        createChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
        if (toneGen != null) {
            toneGen.release();
            toneGen = null;
        }
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Tập thở đang chạy", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Thông báo khi đang tập thở");
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !running) {
            inhale = intent.getIntExtra("inhale", 4);
            hold = intent.getIntExtra("hold", 4);
            exhale = intent.getIntExtra("exhale", 4);
            totalCycles = intent.getIntExtra("cycles", 10);
            currentCycle = 1;
            phaseIndex = 0;
            remaining = inhale;
            running = true;

            startForeground(NOTIF_ID, buildNotification());
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(3 * 60 * 60 * 1000L); // tối đa 3 tiếng, tự nhả nếu quên
            }
            vibrateShort();
            handler.postDelayed(tick, 1000);
        }
        return START_NOT_STICKY;
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            remaining--;
            if (remaining <= 0) {
                phaseIndex++;
                if (phaseIndex > 2) {
                    phaseIndex = 0;
                    currentCycle++;
                    if (currentCycle > totalCycles) {
                        finishSession();
                        return;
                    }
                }
                remaining = getPhaseDuration(phaseIndex);
                vibrateShort();
            }
            updateNotification();
            if (callback != null) {
                callback.onTick(phaseNames[phaseIndex], remaining, currentCycle, totalCycles);
            }
            handler.postDelayed(this, 1000);
        }
    };

    private int getPhaseDuration(int index) {
        if (index == 0) return inhale;
        if (index == 1) return hold;
        return exhale;
    }

    private void vibrateShort() {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        if (toneGen != null) {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
        }
    }

    private void vibrateLong() {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        if (toneGen != null) {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 600);
        }
    }

    private void finishSession() {
        running = false;
        handler.removeCallbacks(tick);
        vibrateLong();
        if (callback != null) {
            callback.onFinished();
        }
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    public void stopSession() {
        running = false;
        handler.removeCallbacks(tick);
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public String getCurrentPhaseName() {
        return phaseNames[phaseIndex];
    }

    public int getRemaining() {
        return remaining;
    }

    public int getCurrentCycle() {
        return currentCycle;
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Đang tập thở")
                .setContentText(phaseNames[phaseIndex] + " - Chu kỳ " + currentCycle + "/" + totalCycles)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setOngoing(true)
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(NOTIF_ID, buildNotification());
    }

    public void setCallback(TimerCallback cb) {
        this.callback = cb;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Vuốt tắt app khỏi danh sách gần đây -> tự động kết thúc phiên tập
        running = false;
        handler.removeCallbacks(tick);
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
