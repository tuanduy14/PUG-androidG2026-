package com.example.pugcore;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.util.Locale;


public class PlankActivity extends AppCompatActivity {

    private static final double OK_MIN = PostureBarView.OK_MIN, OK_MAX = 180.0;
    private static final float MIN_VIS = 0.5f;
    private static final long WARN_DELAY_MS = 300;      // lưng sai quá ngần này mới hiện cảnh báo (tránh nháy)
    private static final long SIGNAL_TIMEOUT_MS = 1200; // quá lâu không có khung hình mới thì coi như mất tín hiệu
    private static final long SAVE_EVERY_MS = 3000;

    private PreviewView viewFinder;
    private PoseOverlayView poseOverlay;
    private FrameLayout popupLayer;
    private TextView tvHold, tvExp, tvLevel, tvStatus, tvWarning, tvBanner, tvQuest, tvAngle;
    private PixelBarView pbExp;
    private PostureBarView postureBar;
    private QuestBarView questBar;

    private PoseTracker tracker;
    private PlayerData player;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long lastFrameAt = 0, invalidSince = 0, lastSaveAt = 0, sessionValidMs = 0;
    private int sessionExp = 0;
    private double smoothed = -1;
    private boolean prevValid = false;
    private boolean everSawFrame = false;

    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            long now = SystemClock.uptimeMillis();
            if (everSawFrame && now - lastFrameAt > SIGNAL_TIMEOUT_MS) {
                prevValid = false;
                showWarning("MẤT TÍN HIỆU CAMERA\nKiểm tra lại camera");
                setStatus("TẠM DỪNG", 0xFFFDA4AF);
            }
            handler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    tracker.startCamera(this, viewFinder);
                } else {
                    Toast.makeText(this, "Cần quyền camera để tập Plank", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PixelUi.init(this);
        SoundFx.init(this);
        setContentView(R.layout.activity_plank);
        UiUtil.applyOverlayInsets(findViewById(R.id.root),
                findViewById(R.id.topOverlay), findViewById(R.id.bottomBar));
        SettingsDialog.attach(this);

        viewFinder = findViewById(R.id.viewFinder);
        poseOverlay = findViewById(R.id.poseOverlay);
        popupLayer = findViewById(R.id.popupLayer);
        tvHold = findViewById(R.id.tvHold);
        tvExp = findViewById(R.id.tvExp);
        tvLevel = findViewById(R.id.tvLevel);
        tvStatus = findViewById(R.id.tvStatus);
        tvWarning = findViewById(R.id.tvWarning);
        tvBanner = findViewById(R.id.tvBanner);
        tvQuest = findViewById(R.id.tvQuest);
        tvAngle = findViewById(R.id.tvAngle);
        pbExp = findViewById(R.id.pbExp);
        postureBar = findViewById(R.id.postureBar);
        questBar = findViewById(R.id.questBar);
        pbExp.setFillColor(0xFF38BDF8);

        findViewById(R.id.topOverlay).setBackground(PixelUi.box(this, 0xCC161C2C, PixelUi.OUTLINE, 3));
        findViewById(R.id.bottomBar).setBackground(PixelUi.box(this, 0xCC161C2C, PixelUi.OUTLINE, 3));
        tvWarning.setBackground(PixelUi.box(this, 0xE6991B1B, 0xFFFB7185, 3));
        TextView btnFinish = findViewById(R.id.btnFinish);
        btnFinish.setBackground(PixelUi.button(this, 0xFFF59E0B));
        btnFinish.setOnClickListener(v -> finish());

        player = PlayerData.load(this);

        // Rep hít đất không dùng ở đây; PoseTracker chỉ cần chạy để lấy khung xương
        tracker = new PoseTracker(this, () -> { });
        tracker.setFrameListener(this::onFrame);
        tracker.setCounting(true);
        setStatus("ĐANG KHỞI ĐỘNG CAMERA...", 0xFFCBD5E1);
        updateUi();
        PixelUi.applyFont(findViewById(android.R.id.content));

        if (!tracker.isReady()) {
            Toast.makeText(this, "MediaPipe không chạy được trên thiết bị này.", Toast.LENGTH_LONG).show();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            tracker.startCamera(this, viewFinder);
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // ------------------------------------------------------------------ nhận diện tư thế

    private static float minVis(PoseFrame f, int a, int b, int c) {
        return Math.min(f.visibility[a], Math.min(f.visibility[b], f.visibility[c]));
    }

    /** Góc tại hông (độ) giữa hai đoạn hông→vai và hông→cổ chân, tính theo pixel thật của ảnh. */
    private static double hipAngle(PoseFrame f, int s, int h, int a) {
        double w = f.width, hh = f.height;
        double ax = (f.x[s] - f.x[h]) * w, ay = (f.y[s] - f.y[h]) * hh;
        double bx = (f.x[a] - f.x[h]) * w, by = (f.y[a] - f.y[h]) * hh;
        double ang = Math.abs(Math.toDegrees(Math.atan2(by, bx) - Math.atan2(ay, ax)));
        return ang > 180.0 ? 360.0 - ang : ang;
    }

    /** Phân biệt hông bị hạ (võng lưng) với chổng mông dựa vào vị trí hông so với đường vai–cổ chân. */
    private static String postureHint(PoseFrame f, int s, int h, int a) {
        double w = f.width, hh = f.height;
        double sx = f.x[s] * w, sy = f.y[s] * hh, hx = f.x[h] * w, hy = f.y[h] * hh;
        double ax = f.x[a] * w, ay = f.y[a] * hh;
        double dx = ax - sx;
        if (Math.abs(dx) < 0.05 * w) return "GIỮ THẲNG LƯNG\nGóc cần từ 165° đến 180°";
        double lineY = sy + (ay - sy) * ((hx - sx) / dx);
        return hy > lineY
                ? "HẠ HÔNG! NÂNG HÔNG LÊN\nGiữ thẳng lưng (165°–180°)"
                : "CHỔNG MÔNG! HẠ HÔNG XUỐNG\nGiữ thẳng lưng (165°–180°)";
    }

    private void onFrame(PoseFrame f) {
        long now = SystemClock.uptimeMillis();
        long dt = lastFrameAt == 0 ? 0 : Math.min(now - lastFrameAt, 400);
        lastFrameAt = now;
        everSawFrame = true;
        poseOverlay.setFrame(f);

        double angleNow = Double.NaN;
        boolean valid = false;
        String message = null;

        if (!f.hasPerson() || f.x.length <= 28) {
            smoothed = -1;
            message = "KHÔNG THẤY NGƯỜI\nĐặt máy nhìn ngang bên hông bạn";
        } else {
            float left = minVis(f, 11, 23, 27), right = minVis(f, 12, 24, 28);
            boolean useLeft = left >= right;
            int s = useLeft ? 11 : 12, h = useLeft ? 23 : 24, a = useLeft ? 27 : 28;
            if ((useLeft ? left : right) < MIN_VIS) {
                smoothed = -1;
                message = "CHƯA THẤY RÕ VAI – HÔNG – CỔ CHÂN\nLùi ra để camera thấy toàn thân";
            } else {
                double raw = hipAngle(f, s, h, a);
                smoothed = smoothed < 0 ? raw : 0.6 * smoothed + 0.4 * raw;
                angleNow = smoothed;
                valid = smoothed >= OK_MIN && smoothed <= OK_MAX;
                if (!valid) message = postureHint(f, s, h, a);
            }
        }

        // chỉ cộng thời gian khi cả khung trước và khung này đều chuẩn
        if (valid && prevValid) addTime(dt);
        prevValid = valid;

        if (valid) {
            invalidSince = 0;
            hideWarning();
            setStatus("TƯ THẾ CHUẨN – ĐANG TÍNH GIỜ", 0xFF6EE7B7);
        } else {
            if (invalidSince == 0) invalidSince = now;
            setStatus("TẠM DỪNG", 0xFFFDA4AF);
            if (now - invalidSince >= WARN_DELAY_MS && message != null) showWarning(message);
        }

        postureBar.setAngle(angleNow, valid);
        tvAngle.setText(Double.isNaN(angleNow) ? "Góc lưng: --"
                : String.format(Locale.getDefault(), "Góc lưng: %.0f°", angleNow));
        updateUi();
    }

    // ------------------------------------------------------------------ thưởng

    private void addTime(long dt) {
        if (dt <= 0) return;
        sessionValidMs += dt;
        int[] r = player.addPlankTime(dt); // {EXP, vé, số cấp lên}
        boolean dirty = false;

        if (r[0] > 0) {
            sessionExp += r[0];
            SoundFx.play(SoundFx.TRAIN_CLICK);
            Fx.popup(popupLayer, "+" + r[0] + " EXP", 0xFF7DD3FC, 18, 0.5f, 0.35f);
            dirty = true;
        }
        if (r[2] > 0) {
            SoundFx.play(SoundFx.LEVEL_UP);
            showBanner("LEVEL UP! LV." + player.level);
        }
        if (r[1] > 0) {
            SoundFx.play(SoundFx.TICKET);
            showBanner("ĐẠT MỐC PLANK · +" + r[1] + " VÉ GACHA!");
            dirty = true;
        }
        long now = SystemClock.uptimeMillis();
        if (dirty || now - lastSaveAt > SAVE_EVERY_MS) {
            player.save(this);
            lastSaveAt = now;
        }
    }

    // ------------------------------------------------------------------ giao diện

    private static String mmss(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    private void updateUi() {
        player.rollDaily(); // qua 00:00 thì tiến độ ngày về 0
        tvHold.setText(mmss(sessionValidMs));
        long left = PlayerData.PLANK_MS_PER_EXP - player.plankRemainderMs;
        tvExp.setText(String.format(Locale.getDefault(), "+%d EXP · còn %.1fs → +1", sessionExp, left / 1000f));
        tvLevel.setText(String.format(Locale.getDefault(), "LV.%d  EXP %d/%d", player.level, player.exp, player.expToNext()));
        pbExp.setFraction(player.exp / (float) player.expToNext(), true);
        questBar.setProgress(player.plankDailyMs, player.plankClaimedMask);
        tvQuest.setText("Hôm nay: " + mmss(Math.min(player.plankDailyMs, PlayerData.PLANK_GOAL_S * 1000))
                + " / 5:00 · Vé: " + player.gachaTickets);
    }

    private void setStatus(String text, int color) {
        tvStatus.setText(text);
        tvStatus.setTextColor(color);
    }

    private void showWarning(String text) {
        tvWarning.setText(text);
        tvWarning.setVisibility(View.VISIBLE);
    }

    private void hideWarning() {
        tvWarning.setVisibility(View.GONE);
    }

    private void showBanner(String text) {
        tvBanner.setText(text);
        tvBanner.animate().cancel();
        tvBanner.setVisibility(View.VISIBLE);
        tvBanner.setAlpha(0f);
        tvBanner.setScaleX(0.75f);
        tvBanner.setScaleY(0.75f);
        tvBanner.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220)
                .withEndAction(() -> tvBanner.animate().alpha(0f).setStartDelay(1600).setDuration(300)
                        .withEndAction(() -> tvBanner.setVisibility(View.GONE)).start())
                .start();
    }

    // ------------------------------------------------------------------ vòng đời

    @Override
    protected void onResume() {
        super.onResume();
        MusicPlayer.play(this, MusicPlayer.TRAIN);
        handler.removeCallbacks(watchdog);
        handler.postDelayed(watchdog, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicPlayer.pause();
        handler.removeCallbacks(watchdog);
        player.save(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        player.save(this);
        tracker.release();
    }
}
