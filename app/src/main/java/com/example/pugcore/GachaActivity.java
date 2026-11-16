package com.example.pugcore;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Gacha (dọc): rương báu pixel, QUAY x1 (1 vé) / QUAY x10 (10 vé), bảo hiểm, nhiệm vụ Plank hằng ngày. */
public class GachaActivity extends AppCompatActivity {

    private PlayerData player;
    private ChestView chest;
    private SparkleView sparkle;
    private View flash;
    private FrameLayout chestFrame;
    private TextView tvTickets, tvPity, tvQuestInfo, btnPull1, btnPull10;
    private PixelBarView barPity;
    private QuestBarView questBar;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private final List<Animator> animators = new ArrayList<>();
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PixelUi.init(this);
        SoundFx.init(this);
        setContentView(R.layout.activity_gacha);
        UiUtil.applySystemBarPadding(findViewById(R.id.root));
        SettingsDialog.attach(this);

        ((SceneBackgroundView) findViewById(R.id.gachaBg)).setScene(SceneBackgroundView.TITLE, false);
        chest = findViewById(R.id.chestView);
        sparkle = findViewById(R.id.sparkleView);
        flash = findViewById(R.id.flashOverlay);
        chestFrame = findViewById(R.id.chestFrame);
        tvTickets = findViewById(R.id.tvTickets);
        tvPity = findViewById(R.id.tvPity);
        tvQuestInfo = findViewById(R.id.tvQuestInfo);
        btnPull1 = findViewById(R.id.btnPull1);
        btnPull10 = findViewById(R.id.btnPull10);
        barPity = findViewById(R.id.barPity);
        questBar = findViewById(R.id.questBar);
        barPity.setFillColor(0xFFFBBF24);

        findViewById(R.id.ticketBox).setBackground(PixelUi.box(this, 0xFF0F1320, PixelUi.OUTLINE, 2));
        findViewById(R.id.pityPanel).setBackground(PixelUi.box(this, 0xE61C2233, PixelUi.OUTLINE, 3));
        findViewById(R.id.questPanel).setBackground(PixelUi.box(this, 0xE61C2233, PixelUi.OUTLINE, 3));

        TextView back = findViewById(R.id.btnBack);
        back.setBackground(PixelUi.button(this, 0xFF28314B));
        back.setOnClickListener(v -> { SoundFx.play(SoundFx.TAB); finish(); });

        btnPull1.setBackground(PixelUi.button(this, PixelUi.AMBER));
        btnPull10.setBackground(PixelUi.button(this, 0xFF9333EA));
        btnPull1.setOnClickListener(v -> pull(1));
        btnPull10.setOnClickListener(v -> pull(10));

        TextView btnPlank = findViewById(R.id.btnPlank);
        TextView btnRates = findViewById(R.id.btnRates);
        TextView btnInv = findViewById(R.id.btnInventory);
        btnPlank.setBackground(PixelUi.button(this, 0xFF10B981));
        btnRates.setBackground(PixelUi.button(this, 0xFF28314B));
        btnInv.setBackground(PixelUi.button(this, 0xFF0EA5E9));
        btnPlank.setOnClickListener(v -> goPlank());
        btnRates.setOnClickListener(v -> showRates());
        btnInv.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            startActivity(new Intent(this, InventoryActivity.class));
        });

        PixelUi.applyFont(findViewById(android.R.id.content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicPlayer.play(this, MusicPlayer.MENU);
        player = PlayerData.load(this);
        refreshUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        cancelAnimators();
    }

    private void cancelAnimators() {
        for (Animator a : animators) a.cancel();
        animators.clear();
    }

    private void goPlank() {
        SoundFx.play(SoundFx.TAB);
        UiUtil.askRotateThen(this, "thấy rõ vai, hông và cổ chân của bạn",
                () -> startActivity(new Intent(this, PlankActivity.class)));
    }

    private static String mmss(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    private void refreshUi() {
        player.rollDaily();
        tvTickets.setText(String.valueOf(player.gachaTickets));
        tvPity.setText(player.gachaPity + "/" + GachaEngine.PITY_LEGEND);
        barPity.setFraction(player.gachaPity / (float) GachaEngine.PITY_LEGEND, true);
        questBar.setProgress(player.plankDailyMs, player.plankClaimedMask);
        tvQuestInfo.setText("Hôm nay: " + mmss(Math.min(player.plankDailyMs, PlayerData.PLANK_GOAL_S * 1000))
                + " / 5:00 · tối đa 10 vé/ngày · làm mới lúc 00:00");
        btnPull1.setAlpha(busy || player.gachaTickets < 1 ? 0.55f : 1f);
        btnPull10.setAlpha(busy || player.gachaTickets < 10 ? 0.55f : 1f);
    }

    // ------------------------------------------------------------------ quay

    private void pull(int count) {
        if (busy) return;
        if (player.gachaTickets < count) {
            showNoTickets(count);
            return;
        }
        busy = true;
        player.gachaTickets -= count;
        final List<GachaEngine.Drop> drops = GachaEngine.pull(player, count, rnd);
        player.save(this);
        refreshUi();

        GameData.Rarity best = null;
        boolean jackpot = false;
        for (GachaEngine.Drop d : drops) {
            if (d.gold >= 100) jackpot = true;
            GameData.Rarity r = d.rarity();
            if (r != null && (best == null || r.ordinal() > best.ordinal())) best = r;
        }
        final GameData.Rarity bestRarity = best;
        final boolean isJackpot = jackpot;

        SoundFx.play(SoundFx.GACHA_PULL);
        chest.setOpen(false, 0);
        Fx.shake(chestFrame);
        handler.postDelayed(() -> Fx.shake(chestFrame), 260);
        handler.postDelayed(() -> Fx.shake(chestFrame), 520);
        handler.postDelayed(() -> openChest(drops, bestRarity, isJackpot), 820);
    }

    private void openChest(List<GachaEngine.Drop> drops, GameData.Rarity best, boolean jackpot) {
        int glow;
        if (best == GameData.Rarity.LEGENDARY) glow = 0xFFFBBF24;
        else if (best == GameData.Rarity.EPIC) glow = 0xFFC084FC;
        else if (best == GameData.Rarity.RARE) glow = 0xFF38BDF8;
        else if (best == GameData.Rarity.COMMON) glow = 0xFFE2E8F0;
        else glow = 0xFFFDE047;
        chest.setOpen(true, glow);

        if (best == GameData.Rarity.LEGENDARY) {
            SoundFx.play(SoundFx.GACHA_LEGEND);
            Fx.flash(flash, 0xFFFFE08A, 3);
            sparkle.start(glow, 24);
        } else if (best == GameData.Rarity.EPIC || jackpot) {
            SoundFx.play(SoundFx.GACHA_EPIC);
            Fx.flash(flash, jackpot && best != GameData.Rarity.EPIC ? 0xFFFFE08A : 0xFFD8B4FE, 2);
            sparkle.start(glow, 16);
        } else {
            SoundFx.play(SoundFx.BUY);
            sparkle.start(glow, 6);
        }
        handler.postDelayed(() -> showResults(drops, best), 1100);
    }

    // ------------------------------------------------------------------ kết quả

    private void showResults(List<GachaEngine.Drop> drops, GameData.Rarity best) {
        if (isFinishing() || isDestroyed()) return;
        int titleColor = best == GameData.Rarity.LEGENDARY ? 0xFFFBBF24
                : best == GameData.Rarity.EPIC ? 0xFFC084FC : PixelUi.AMBER_LIGHT;
        PixelDialog d = new PixelDialog(this)
                .title(drops.size() == 1 ? "KẾT QUẢ QUAY" : "KẾT QUẢ QUAY x" + drops.size(), titleColor);
        if (best == GameData.Rarity.EPIC || best == GameData.Rarity.LEGENDARY) {
            animators.add(Fx.blinkText(d.titleView(), titleColor, 0xFFFFFFFF));
        }
        for (GachaEngine.Drop drop : drops) d.add(resultRow(drop));
        d.setOnDismissListener(x -> {
            cancelAnimators();
            sparkle.stop();
            chest.setOpen(false, 0);
            busy = false;
            refreshUi();
        });
        d.button("XONG", PixelUi.AMBER, PixelUi.DARK_TEXT, null).show();
    }

    private View resultRow(GachaEngine.Drop d) {
        boolean jackpot = !d.isGear() && d.gold >= 100;
        int color;
        String name, sub;
        if (!d.isGear()) {
            color = jackpot ? 0xFFFBBF24 : 0xFFFDE047;
            name = (jackpot ? "NỔ HŨ! " : "") + "+" + d.gold + " VÀNG";
            sub = jackpot ? "Giải đặc biệt!" : "Vàng";
        } else {
            GameData.Rarity r = d.rarity();
            color = r.color;
            name = d.weapon != null ? d.weapon.name : d.armor.name;
            sub = "[" + r.label + "] · "
                    + (d.weapon != null ? "Vũ khí +" + d.weapon.bonusDmg + "/rep" : "Giáp +" + d.armor.bonusHp + " HP")
                    + (d.duplicate ? " · TRÙNG → +" + d.refund + " VÀNG" : " · MỚI");
        }
        boolean special = d.isEpicPlus() || jackpot;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = (int) PixelUi.dp(this, 8);
        row.setPadding(pad, pad, pad, pad);
        row.setBackground(PixelUi.box(this, 0xFF161C2C, d.isGear() || jackpot ? color : PixelUi.OUTLINE, 3));

        FrameLayout iconBox = new FrameLayout(this);
        iconBox.setBackground(PixelUi.box(this, 0xFF101420, color, 2));
        int sz = (int) PixelUi.dp(this, 46);
        PixelSpriteView icon = new PixelSpriteView(this);
        if (!d.isGear()) icon.setIcon("coin", null);
        else if (d.weapon != null) icon.setIcon("sword", d.weapon.blade);
        else icon.setIcon("shield", d.armor.primary);
        int is = (int) PixelUi.dp(this, 28);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(is, is, Gravity.CENTER);
        iconBox.addView(icon, ilp);
        if (special) { // sprite lóe sáng pixel
            SparkleView sv = new SparkleView(this);
            iconBox.addView(sv, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            sv.start(color, 6);
        }
        row.addView(iconBox, new LinearLayout.LayoutParams(sz, sz));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(13);
        tvName.setTextColor(color);
        col.addView(tvName);
        TextView tvSub = new TextView(this);
        tvSub.setText(sub);
        tvSub.setTextSize(11);
        tvSub.setTextColor(PixelUi.SLATE);
        col.addView(tvSub);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = (int) PixelUi.dp(this, 10);
        row.addView(col, clp);

        if (special) animators.add(Fx.blinkText(tvName, color, 0xFFFFFFFF)); // chữ màu nhấp nháy
        return row;
    }

    // ------------------------------------------------------------------ hộp thoại phụ

    private void showNoTickets(int need) {
        new PixelDialog(this)
                .title("KHÔNG ĐỦ VÉ", 0xFFFB7185)
                .message(String.format(Locale.getDefault(),
                        "Cần %d vé nhưng bạn chỉ có %d.\n\nTập Plank mỗi ngày để nhận tối đa 10 vé!",
                        need, player.gachaTickets))
                .button("TẬP PLANK", 0xFF10B981, PixelUi.DARK_TEXT, this::goPlank)
                .button("ĐÓNG", 0xFF2A334D, PixelUi.TEXT, null)
                .show();
    }

    private void showRates() {
        SoundFx.play(SoundFx.TAB);
        new PixelDialog(this)
                .title("TỈ LỆ QUAY", PixelUi.AMBER_LIGHT)
                .message("VÀNG: 33%\n"
                        + "  · 2 Vàng: 16%\n  · 5 Vàng: 10%\n  · 10 Vàng: 6%\n  · 100 Vàng (nổ hũ): 1%\n\n"
                        + "TRANG BỊ ĐỘC QUYỀN GACHA\n"
                        + "  · Thường: 40%\n  · Hiếm: 18.5%\n  · Sử thi: 7%\n  · Huyền thoại: 1.5%\n\n"
                        + "BẢO HIỂM\n"
                        + "  · Quay x10: chắc chắn có ≥ 1 trang bị Sử thi trở lên.\n"
                        + "  · Quay đủ 70 lần: chắc chắn có 1 trang bị Huyền thoại (ra Huyền thoại thì đếm lại từ 0).\n\n"
                        + "TRÙNG TRANG BỊ → ĐỔI RA VÀNG\n"
                        + "  · Thường: 2 Vàng\n  · Hiếm: 5 Vàng\n  · Sử thi: 10 Vàng\n  · Huyền thoại: 50 Vàng")
                .button("ĐÓNG", PixelUi.AMBER, PixelUi.DARK_TEXT, null)
                .show();
    }
}
