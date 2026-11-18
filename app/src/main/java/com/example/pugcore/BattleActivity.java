package com.example.pugcore;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
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
    private PixelBarView pbMonster, pbPlayer;
    private TextView btnAttack, btnPotion, btnMode, btnExit;
    private PixelSpriteView monsterSprite, heroSprite;
    private FrameLayout monsterBox, heroBox;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Chế độ không giới hạn: quá ngần này ms không có rep mới thì tự kết thúc lượt. */
    private static final long IDLE_LIMIT_MS = 5000;
    private long lastRepTime = 0;
    private final Runnable idleTick = new Runnable() {
        @Override public void run() {
            if (state != State.COUNTING) return;
            if (reps == 0) { // chưa có rep nào: chờ người chơi vào tư thế, không tính giờ
                tvTimer.setText("Hãy bắt đầu!");
            } else {
                long left = IDLE_LIMIT_MS - (SystemClock.uptimeMillis() - lastRepTime);
                if (left <= 0) { endTurn(); return; }
                tvTimer.setText(String.format(Locale.getDefault(), "Nghỉ %.1fs", left / 1000f));
            }
            handler.postDelayed(this, 100);
        }
    };

    private PoseTracker tracker;
    private PoseOverlayView poseOverlay;
    private TextView tvDebug;
    private PlayerData player;
    private GameData.Monster monster;
    private CountDownTimer timer;

    private static final String TAG = "PUG_BATTLE";

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
        PixelUi.init(this);
        SoundFx.init(this);
        setContentView(R.layout.activity_battle);
        UiUtil.applyOverlayInsets(findViewById(R.id.root),
                findViewById(R.id.topOverlay), findViewById(R.id.bottomBar));
        SettingsDialog.attach(this);

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
        btnMode = findViewById(R.id.btnMode);
        btnExit = findViewById(R.id.btnExit);
        monsterSprite = findViewById(R.id.monsterSprite);
        heroSprite = findViewById(R.id.heroSprite);
        monsterBox = findViewById(R.id.monsterBox);
        heroBox = findViewById(R.id.heroBox);

        player = PlayerData.load(this);
        int idx = getIntent().getIntExtra(EXTRA_MONSTER, 0);
        idx = Math.max(0, Math.min(idx, GameData.MONSTERS.length - 1));
        monster = GameData.MONSTERS[idx];

        monsterHp = monster.maxHp;
        maxPlayerHp = player.maxHp();
        playerHp = maxPlayerHp;
        pbMonster.setFillColor(0xFFE11D48);
        pbPlayer.setFillColor(0xFFF43F5E);
        findViewById(R.id.topOverlay).setBackground(PixelUi.box(this, 0xCC161C2C, PixelUi.OUTLINE, 3));
        findViewById(R.id.bottomBar).setBackground(PixelUi.box(this, 0xCC161C2C, PixelUi.OUTLINE, 3));
        btnAttack.setBackground(PixelUi.button(this, PixelUi.AMBER));
        btnAttack.setTextColor(PixelUi.DARK_TEXT);
        btnPotion.setBackground(PixelUi.button(this, 0xFF222A3F));
        btnPotion.setTextColor(0xFFF1F5F9);
        btnMode.setBackground(PixelUi.button(this, 0xFF0EA5E9));
        btnMode.setTextColor(PixelUi.DARK_TEXT);
        btnMode.setOnClickListener(v -> showModeDialog());
        btnExit.setBackground(PixelUi.button(this, 0xFFF43F5E));
        btnExit.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            confirmExit();
        });
        // nút Back của hệ thống cũng hỏi xác nhận thay vì thoát luôn
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmExit(); }
        });
        monsterSprite.setMonster(monster.archetype);
        heroSprite.setHero(PixelArt.POSE_IDLE, GameData.ARMORS[player.armorId], GameData.WEAPONS[player.weaponId]);
        Fx.bob(monsterSprite);

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

    // ---------- Lượt của người chơi ----------

    private void startTurn() {
        state = State.COUNTING;
        reps = 0;
        if (timer != null) { timer.cancel(); timer = null; }
        tracker.setCounting(true);
        btnAttack.setText("Kết thúc lượt");

        if (player.turnMode == PlayerData.MODE_FREE) {
            handler.post(idleTick);
        } else {
            // Đồng hồ chỉ chạy khi đếm được rep đầu tiên (xem startTimedCountdown)
            tvTimer.setText("Sẵn sàng: " + player.turnSeconds + "s");
        }
        updateUi();
    }

    /** Chế độ tự đặt thời gian: bắt đầu đếm ngược từ lúc có rep đầu tiên. */
    private void startTimedCountdown() {
        timer = new CountDownTimer(player.turnSeconds * 1000L, 250) {
            @Override
            public void onTick(long ms) {
                tvTimer.setText("Còn " + ((ms + 999) / 1000) + "s");
            }

            @Override
            public void onFinish() {
                endTurn();
            }
        }.start();
    }

    private void onRep() {
        if (state != State.COUNTING) return;
        reps++;
        lastRepTime = SystemClock.uptimeMillis();
        player.addPushUp();
        player.save(this);
        SoundFx.play(SoundFx.TAP_ATTACK);
        if (reps == 1 && player.turnMode == PlayerData.MODE_TIMED) startTimedCountdown();
        updateUi();
    }

    private void endTurn() {
        if (state != State.COUNTING) return;
        if (timer != null) { timer.cancel(); timer = null; }
        handler.removeCallbacks(idleTick);
        tracker.setCounting(false);
        tvTimer.setText("");
        poseOverlay.setFrame(null);
        tvDebug.setText("");

        int dmg = player.calcDamage(reps);
        int hpBefore = monsterHp;
        monsterHp = Math.max(0, monsterHp - dmg);
        Log.d(TAG, "endTurn reps=" + reps + " dmg=" + dmg + " monsterHp " + hpBefore + "->" + monsterHp
                + "/" + monster.maxHp + " playerHp=" + playerHp + "/" + maxPlayerHp);
        log("Bạn làm " + reps + " rep, gây " + dmg + " sát thương! Quái còn " + monsterHp + "/" + monster.maxHp + " máu.");
        SoundFx.play(SoundFx.HEAVY_STRIKE);
        heroSprite.setHeroPose(PixelArt.POSE_ATTACK);
        monsterSprite.setHurt(true);
        Fx.lungeUp(heroBox);
        Fx.shake(monsterBox);
        Fx.popup(monsterBox, "-" + dmg, 0xFFFCD34D, 22, 0.5f, 0.1f);
        handler.postDelayed(() -> {
            heroSprite.setHeroPose(PixelArt.POSE_IDLE);
            monsterSprite.setHurt(false);
        }, 400);

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
        SoundFx.play(SoundFx.POTION);
        Fx.popup(heroBox, "+" + healed + " HP", 0xFF34D399, 18, 0.5f, 0.0f);
        monsterTurn(); // dùng bình tốn 1 lượt
    }

    // ---------- Lượt của quái ----------

    private void monsterTurn() {
        playerHp = Math.max(0, playerHp - monster.attack);
        log(monster.name + " phản công, bạn mất " + monster.attack + " máu!");
        SoundFx.play(SoundFx.MONSTER_HIT);
        Fx.lungeDown(monsterBox);
        Fx.shake(heroBox);
        heroSprite.setHeroPose(PixelArt.POSE_HURT);
        Fx.popup(heroBox, "-" + monster.attack, 0xFFFB7185, 20, 0.5f, 0.0f);
        handler.postDelayed(() -> heroSprite.setHeroPose(PixelArt.POSE_IDLE), 400);

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
        if (state == State.OVER) return;
        if (monsterHp > 0) { // an toàn: chỉ thắng khi quái thật sự hết máu
            Log.e(TAG, "win() bị gọi nhưng quái còn " + monsterHp + " máu — bỏ qua");
            return;
        }
        Log.d(TAG, "WIN stage=" + monster.stage);
        state = State.OVER;
        SoundFx.play(SoundFx.VICTORY);
        // chỉ lần thắng đầu tiên của ván này mới có thưởng; đánh lại thì không
        boolean firstClear = !player.isCleared(monster.stage);
        if (firstClear) player.gold += monster.rewardGold;
        player.markCleared(monster.stage);
        player.save(this);
        updateUi();
        showEndDialog(true, firstClear
                ? "Bạn hạ gục " + monster.name + " lần đầu và nhận " + monster.rewardGold + " vàng."
                : "Bạn hạ gục " + monster.name + ". Ván này đã thắng trước đó nên không nhận thưởng.");
    }

    private void lose() {
        if (state == State.OVER) return;
        Log.d(TAG, "LOSE stage=" + monster.stage + " monsterHp=" + monsterHp + "/" + monster.maxHp);
        state = State.OVER;
        SoundFx.play(SoundFx.GAME_OVER);
        updateUi();
        showEndDialog(false, "Bạn đã gục ngã trước " + monster.name + ". Quái còn " + monsterHp + "/" + monster.maxHp + " máu.");
    }

    private void showEndDialog(boolean won, String message) {
        if (isFinishing() || isDestroyed()) return;
        new PixelDialog(this)
                .title(won ? "CHIẾN THẮNG!" : "THẤT BẠI", won ? PixelUi.AMBER_LIGHT : 0xFFFB7185)
                .message(message)
                .noCancel()
                .button("VỀ BẢN ĐỒ", won ? PixelUi.AMBER : 0xFFF43F5E,
                        won ? PixelUi.DARK_TEXT : 0xFFFFFFFF, this::finish)
                .show();
    }

    // ---------- UI ----------

    private void log(String line) {
        logs.add(line);
        int from = Math.max(0, logs.size() - 3);
        tvLog.setText(TextUtils.join("\n", logs.subList(from, logs.size())));
    }

    private void updateUi() {
        pbMonster.setFraction(monsterHp / (float) monster.maxHp, true);
        pbPlayer.setFraction(playerHp / (float) maxPlayerHp, true);
        tvMonster.setText(monster.name + "  " + monsterHp + "/" + monster.maxHp);
        tvPlayer.setText("Bạn (Cấp " + player.level + ")  " + playerHp + "/" + maxPlayerHp);

        if (state == State.COUNTING) {
            tvReps.setText("Rep: " + reps + "  →  " + player.calcDamage(reps) + " sát thương");
        } else {
            tvReps.setText("");
        }

        btnMode.setText(player.turnMode == PlayerData.MODE_FREE ? "Chế độ: Tự do" : "Chế độ: " + player.turnSeconds + "s");
        PixelUi.setEnabledLook(btnMode, state == State.IDLE);
        PixelUi.setEnabledLook(btnExit, state != State.COUNTING);
        PixelUi.setEnabledLook(btnAttack, state != State.OVER);
        btnPotion.setText("Bình máu (" + player.potions + ")");
        PixelUi.setEnabledLook(btnPotion, state == State.IDLE && player.potions > 0 && playerHp < maxPlayerHp);
    }


    /** Thoát trận đấu (về bản đồ). Đang đếm rep thì phải kết thúc lượt trước, để không mất lượt giữa chừng. */
    private void confirmExit() {
        if (isFinishing() || isDestroyed()) return;
        if (state == State.OVER) { finish(); return; }
        if (state == State.COUNTING) {
            Toast.makeText(this, "Bấm \"Kết thúc lượt\" trước khi thoát", Toast.LENGTH_SHORT).show();
            return;
        }
        new PixelDialog(this)
                .title("THOÁT TRẬN ĐẤU?", PixelUi.AMBER_LIGHT)
                .message("Trận này chưa kết thúc. Nếu thoát, quái sẽ hồi đầy máu khi bạn quay lại.\n"
                        + "Bình máu đã dùng sẽ không được hoàn lại.")
                .button("Ở LẠI", PixelUi.AMBER, PixelUi.DARK_TEXT, null)
                .button("THOÁT", 0xFFF43F5E, 0xFFFFFFFF, this::finish)
                .show();
    }

    /** Chọn chế độ lượt: không giới hạn (5s không rep thì dừng) hoặc tự đặt 10–30 giây. */
    private void showModeDialog() {
        if (state != State.IDLE) return;
        final int[] mode = {player.turnMode};
        final int[] secs = {player.turnSeconds};

        PixelDialog d = new PixelDialog(this).title("CHẾ ĐỘ LƯỢT TẤN CÔNG", PixelUi.AMBER_LIGHT);
        final PixelDialog.SliderRow sec = d.slider("THỜI GIAN MỖI LƯỢT",
                PlayerData.MIN_TURN_SECONDS, PlayerData.MAX_TURN_SECONDS, secs[0], " giây", 0xFFFBBF24,
                (v, user) -> secs[0] = v);

        final PixelDialog.Option[] opt = new PixelDialog.Option[2];
        opt[0] = d.option("KHÔNG GIỚI HẠN THỜI GIAN",
                "Tự kết thúc lượt nếu quá 5 giây không có rep mới",
                mode[0] == PlayerData.MODE_FREE, () -> {
                    mode[0] = PlayerData.MODE_FREE;
                    opt[0].setChecked(true);
                    opt[1].setChecked(false);
                    sec.setEnabled(false);
                });
        opt[1] = d.option("TỰ ĐẶT THỜI GIAN",
                "Đồng hồ chỉ chạy từ rep đầu tiên",
                mode[0] == PlayerData.MODE_TIMED, () -> {
                    mode[0] = PlayerData.MODE_TIMED;
                    opt[0].setChecked(false);
                    opt[1].setChecked(true);
                    sec.setEnabled(true);
                });
        sec.setEnabled(mode[0] == PlayerData.MODE_TIMED);

        d.add(opt[0].row).add(opt[1].row).add(sec.row)
                .button("LƯU", PixelUi.AMBER, PixelUi.DARK_TEXT, () -> {
                    player.turnMode = mode[0];
                    player.turnSeconds = secs[0];
                    player.save(this);
                    log(player.turnMode == PlayerData.MODE_FREE
                            ? "Chế độ: không giới hạn (5s không rep thì kết thúc lượt)."
                            : "Chế độ: " + player.turnSeconds + " giây mỗi lượt (tính từ rep đầu tiên).");
                    updateUi();
                })
                .button("HỦY", 0xFF2A334D, PixelUi.TEXT, null)
                .show();
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
        MusicPlayer.play(this, MusicPlayer.BATTLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        handler.removeCallbacksAndMessages(null);
        tracker.release();
    }
}
