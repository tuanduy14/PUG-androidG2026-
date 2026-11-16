package com.example.pugcore;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import java.util.Random;

/** Tập luyện (ngang): mỗi rep hít đất thật = 1 EXP. Hiệp sĩ trên màn hình nâng tạ theo từng rep. */
public class TrainingActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private TextView tvReps, tvLevel, tvLevelBanner;
    private PixelBarView pbExp;
    private PixelSpriteView hero;
    private FrameLayout popupLayer;

    private PoseTracker tracker;
    private PoseOverlayView poseOverlay;
    private TextView tvDebug;
    private PlayerData player;
    private int sessionReps = 0;
    private boolean liftUp = false;
    private final Random rnd = new Random();

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    tracker.startCamera(this, viewFinder);
                } else {
                    Toast.makeText(this, "Cần quyền camera để đếm push-up", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PixelUi.init(this);
        SoundFx.init(this);
        setContentView(R.layout.activity_training);
        UiUtil.applyOverlayInsets(findViewById(R.id.root),
                findViewById(R.id.topOverlay), findViewById(R.id.bottomBar));
        SettingsDialog.attach(this);

        viewFinder = findViewById(R.id.viewFinder);
        tvReps = findViewById(R.id.tvReps);
        tvLevel = findViewById(R.id.tvLevel);
        tvLevelBanner = findViewById(R.id.tvLevelBanner);
        pbExp = findViewById(R.id.pbExp);
        hero = findViewById(R.id.trainHero);
        popupLayer = findViewById(R.id.popupLayer);
        pbExp.setFillColor(0xFF38BDF8);
        findViewById(R.id.topOverlay).setBackground(PixelUi.box(this, 0xCC161C2C, PixelUi.OUTLINE, 3));
        TextView btnFinish = findViewById(R.id.btnFinish);
        btnFinish.setBackground(PixelUi.button(this, 0xFFF59E0B));
        btnFinish.setOnClickListener(v -> finish());

        player = PlayerData.load(this);
        tracker = new PoseTracker(this, this::onRep);
        poseOverlay = findViewById(R.id.poseOverlay);
        tvDebug = findViewById(R.id.tvDebug);
        if (UiUtil.isDebuggable(this)) tvDebug.setVisibility(View.VISIBLE);
        tracker.setFrameListener(this::showPose);
        tracker.setCounting(true);
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

    private void onRep() {
        sessionReps++;
        player.addPushUp();
        int levelsGained = player.addExp(1);
        player.save(this);
        SoundFx.play(SoundFx.TRAIN_CLICK);
        if (levelsGained > 0) {
            SoundFx.play(SoundFx.LEVEL_UP);
            showLevelUp();
        }

        liftUp = !liftUp;
        float d = getResources().getDisplayMetrics().density;
        hero.animate().cancel();
        hero.animate().translationY(liftUp ? -6 * d : 4 * d)
                .scaleX(liftUp ? 1.05f : 1f).scaleY(liftUp ? 1.05f : 1f).setDuration(75).start();
        Fx.popup(popupLayer, "+1 EXP", 0xFF7DD3FC, 18,
                0.5f + (rnd.nextFloat() - 0.5f) * 0.3f, 0.3f + rnd.nextFloat() * 0.15f);
        updateUi();
    }

    private void showLevelUp() {
        tvLevelBanner.setText(String.format(Locale.getDefault(),
                "LEVEL UP! LV.%d\nHệ số ×%.1f", player.level, player.repScale()));
        tvLevelBanner.animate().cancel();
        tvLevelBanner.setVisibility(View.VISIBLE);
        tvLevelBanner.setAlpha(0f);
        tvLevelBanner.setScaleX(0.75f);
        tvLevelBanner.setScaleY(0.75f);
        tvLevelBanner.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220)
                .withEndAction(() -> tvLevelBanner.animate().alpha(0f).setStartDelay(1300).setDuration(300)
                        .withEndAction(() -> tvLevelBanner.setVisibility(View.GONE)).start())
                .start();
    }

    private void updateUi() {
        tvReps.setText("Push-up: " + sessionReps);
        tvLevel.setText(String.format(Locale.getDefault(),
                "LV.%d  •  EXP %d/%d  •  hệ số ×%.1f",
                player.level, player.exp, player.expToNext(), player.repScale()));
        pbExp.setFraction(player.exp / (float) player.expToNext(), true);
        hero.setHero(liftUp ? PixelArt.POSE_TRAIN_UP : PixelArt.POSE_TRAIN_DOWN,
                GameData.ARMORS[player.armorId], GameData.WEAPONS[player.weaponId]);
    }

    /** Vẽ khung xương và hiện số liệu debug (góc, tay, trạng thái). */
    private void showPose(PoseFrame f) {
        poseOverlay.setFrame(f);
        if (tvDebug.getVisibility() != View.VISIBLE) return;
        String size = "\nẢnh MediaPipe nhận: " + f.width + "x" + f.height;
        if (!f.hasPerson()) {
            tvDebug.setText("Không thấy người trong khung hình" + size);
        } else if (Double.isNaN(f.angle)) {
            tvDebug.setText("Thấy người nhưng tay " + f.arm + " chưa rõ (độ tin cậy thấp)" + size);
        } else {
            tvDebug.setText(String.format(Locale.getDefault(), "Tay %s  |  góc %.0f°  |  %s",
                    f.arm, f.angle, f.down ? "ĐÃ HẠ, duỗi >160° để tính rep" : "co <90° để hạ") + size);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicPlayer.play(this, MusicPlayer.TRAIN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tracker.release();
    }
}
