package com.example.pugcore;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Chiến đấu theo lượt.
 * Lượt người chơi: bấm "Tấn công" -> làm push-up trong tối đa TURN_SECONDS giây
 * (hoặc bấm "Kết thúc lượt") -> sát thương = rep × hệ số cấp × (1 + sát thương vũ khí).
 * Sau đó quái phản công. Dùng bình máu cũng tốn 1 lượt.
 */
public class BattleActivity extends AppCompatActivity {

    public static final String EXTRA_MONSTER = "monster";

    private enum State { IDLE, COUNTING, OVER }

    private PreviewView viewFinder;
    private TextView tvMonster, tvPlayer, tvReps, tvTimer, tvLog;
    private ProgressBar pbMonster, pbPlayer;
    private Button btnAttack, btnPotion;

    private PoseTracker tracker;
    private PoseOverlayView poseOverlay;
    private TextView tvDebug;
    private PlayerData player;
    private GameData.Monster monster;
    private CountDownTimer timer;

    private State state = State.IDLE;
    private int monsterHp, playerHp, maxPlayerHp, reps;
    private final List<String> logs = new ArrayList<>();

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    tracker.startCamera(this, viewFinder);
                } else {
                    Toast.makeText(this, "Cần quyền camera để chiến đấu", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);
        UiUtil.applyOverlayInsets(findViewById(R.id.root),
                findViewById(R.id.topOverlay), findViewById(R.id.bottomBar));

        viewFinder = findViewById(R.id.viewFinder);
        tvMonster = findViewById(R.id.tvMonster);
        tvPlayer = findViewById(R.id.tvPlayer);
        tvReps = findViewById(R.id.tvReps);
        tvTimer = findViewById(R.id.tvTimer);
        tvLog = findViewById(R.id.tvLog);
        pbMonster = findViewById(R.id.pbMonster);
        pbPlayer = findViewById(R.id.pbPlayer);
        btnAttack = findViewById(R.id.btnAttack);
        btnPotion = findViewById(R.id.btnPotion);

        player = PlayerData.load(this);
        int idx = getIntent().getIntExtra(EXTRA_MONSTER, 0);
        idx = Math.max(0, Math.min(idx, GameData.MONSTERS.length - 1));
        monster = GameData.MONSTERS[idx];

        monsterHp = monster.maxHp;
        maxPlayerHp = player.maxHp();
        playerHp = maxPlayerHp;
        pbMonster.setMax(monster.maxHp);
        pbPlayer.setMax(maxPlayerHp);

        tracker = new PoseTracker(this, this::onRep);
        poseOverlay = findViewById(R.id.poseOverlay);
        tvDebug = findViewById(R.id.tvDebug);
        if (UiUtil.isDebuggable(this)) tvDebug.setVisibility(View.VISIBLE);
        tracker.setFrameListener(this::showPose);

        btnAttack.setOnClickListener(v -> {
            if (state == State.IDLE) startTurn();
            else if (state == State.COUNTING) endTurn();
        });
        btnPotion.setOnClickListener(v -> usePotion());

        log("Một con " + monster.name + " xuất hiện!");
        log(String.format(Locale.getDefault(), "Mỗi rep gây %.1f sát thương.",
                player.repScale() * player.damagePerRep()));
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

    // ---------- Lượt của người chơi ----------

    private void startTurn() {
        state = State.COUNTING;
        reps = 0;
        tracker.setCounting(true);
        btnAttack.setText("Kết thúc lượt");

        timer = new CountDownTimer(GameData.TURN_SECONDS * 1000L, 250) {
            @Override
            public void onTick(long ms) {
                tvTimer.setText("Còn " + ((ms + 999) / 1000) + "s");
            }

            @Override
            public void onFinish() {
                endTurn();
            }
        }.start();
        updateUi();
    }

    private void onRep() {
        if (state != State.COUNTING) return;
        reps++;
        updateUi();
    }

    private void endTurn() {
        if (state != State.COUNTING) return;
        if (timer != null) timer.cancel();
        tracker.setCounting(false);
        tvTimer.setText("");
        poseOverlay.setFrame(null);
        tvDebug.setText("");

        int dmg = player.calcDamage(reps);
        monsterHp = Math.max(0, monsterHp - dmg);
        log("Bạn làm " + reps + " rep, gây " + dmg + " sát thương!");

        if (monsterHp == 0) {
            win();
        } else {
            monsterTurn();
        }
    }

    private void usePotion() {
        if (state != State.IDLE || player.potions <= 0) return;
        player.potions--;
        player.save(this);

        int healed = Math.min(GameData.POTION_HEAL, maxPlayerHp - playerHp);
        playerHp += healed;
        log("Dùng bình máu, hồi " + healed + " máu.");
        monsterTurn(); // dùng bình tốn 1 lượt
    }

    // ---------- Lượt của quái ----------

    private void monsterTurn() {
        playerHp = Math.max(0, playerHp - monster.attack);
        log(monster.name + " phản công, bạn mất " + monster.attack + " máu!");

        if (playerHp == 0) {
            lose();
            return;
        }
        state = State.IDLE;
        btnAttack.setText("Tấn công");
        updateUi();
    }

    // ---------- Kết thúc trận ----------

    private void win() {
        state = State.OVER;
        player.gold += monster.rewardGold;
        player.save(this);
        updateUi();
        showEndDialog("Chiến thắng!", "Bạn hạ gục " + monster.name
                + " và nhận " + monster.rewardGold + " vàng.");
    }

    private void lose() {
        state = State.OVER;
        updateUi();
        showEndDialog("Thất bại", "Bạn đã gục ngã trước " + monster.name + ".");
    }

    private void showEndDialog(String title, String message) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
    }

    // ---------- UI ----------

    private void log(String line) {
        logs.add(line);
        int from = Math.max(0, logs.size() - 3);
        tvLog.setText(TextUtils.join("\n", logs.subList(from, logs.size())));
    }

    private void updateUi() {
        pbMonster.setProgress(monsterHp);
        pbPlayer.setProgress(playerHp);
        tvMonster.setText(monster.name + "  " + monsterHp + "/" + monster.maxHp);
        tvPlayer.setText("Bạn (Cấp " + player.level + ")  " + playerHp + "/" + maxPlayerHp);

        if (state == State.COUNTING) {
            tvReps.setText("Rep: " + reps + "  →  " + player.calcDamage(reps) + " sát thương");
        } else {
            tvReps.setText("");
        }

        btnAttack.setEnabled(state != State.OVER);
        btnPotion.setText("Bình máu (" + player.potions + ")");
        btnPotion.setEnabled(state == State.IDLE && player.potions > 0 && playerHp < maxPlayerHp);
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
        if (timer != null) timer.cancel();
        tracker.release();
    }
}
