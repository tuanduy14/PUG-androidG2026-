package com.example.pugcore;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.util.Locale;

/** Tập luyện: mỗi rep = 1 EXP. */
public class TrainingActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private TextView tvReps, tvLevel;
    private ProgressBar pbExp;

    private PoseTracker tracker;
    private PoseOverlayView poseOverlay;
    private TextView tvDebug;
    private PlayerData player;
    private int sessionReps = 0;

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
        setContentView(R.layout.activity_training);
        UiUtil.applyOverlayInsets(findViewById(R.id.root),
                findViewById(R.id.topOverlay), findViewById(R.id.bottomBar));

        viewFinder = findViewById(R.id.viewFinder);
        tvReps = findViewById(R.id.tvReps);
        tvLevel = findViewById(R.id.tvLevel);
        pbExp = findViewById(R.id.pbExp);
        findViewById(R.id.btnFinish).setOnClickListener(v -> finish());

        player = PlayerData.load(this);
        tracker = new PoseTracker(this, this::onRep);
        poseOverlay = findViewById(R.id.poseOverlay);
        tvDebug = findViewById(R.id.tvDebug);
        if (UiUtil.isDebuggable(this)) tvDebug.setVisibility(View.VISIBLE);
        tracker.setFrameListener(this::showPose);
        tracker.setCounting(true);
        updateUi();

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
        int levelsGained = player.addExp(1);
        player.save(this);
        if (levelsGained > 0) {
            Toast.makeText(this, "Lên cấp " + player.level + "!", Toast.LENGTH_SHORT).show();
        }
        updateUi();
    }

    private void updateUi() {
        tvReps.setText("Push-up: " + sessionReps);
        tvLevel.setText(String.format(Locale.getDefault(),
                "Cấp %d  •  EXP %d/%d  •  hệ số ×%.1f",
                player.level, player.exp, player.expToNext(), player.repScale()));
        pbExp.setMax(player.expToNext());
        pbExp.setProgress(player.exp);
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
    protected void onDestroy() {
        super.onDestroy();
        tracker.release();
    }
}
