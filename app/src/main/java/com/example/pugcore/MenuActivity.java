package com.example.pugcore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/** Menu chính (dọc): Tập luyện (Hít đất / Plank), Chiến đấu, Cửa hàng, Gacha, Kho đồ. */
public class MenuActivity extends AppCompatActivity {

    private PixelSpriteView hero;
    private TextView tvGold, tvLevel, tvExp, tvHp, tvDamage, tvPushups, tvPlankTime;
    private PixelBarView barExp, barHp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PixelUi.init(this);
        SoundFx.init(this);
        setContentView(R.layout.activity_menu);
        UiUtil.applySystemBarPadding(findViewById(R.id.root));
        SettingsDialog.attach(this);

        ((SceneBackgroundView) findViewById(R.id.menuBg)).setScene(SceneBackgroundView.TITLE, false);
        findViewById(R.id.goldBox).setBackground(PixelUi.box(this, 0xE60F1320, PixelUi.OUTLINE, 2));
        findViewById(R.id.levelBox).setBackground(PixelUi.box(this, 0xE61C2233, PixelUi.OUTLINE, 3));
        findViewById(R.id.heroInfo).setBackground(PixelUi.box(this, 0xCC161C2C, PixelUi.OUTLINE, 3));
        findViewById(R.id.statsPanel).setBackground(PixelUi.box(this, 0xE61C2233, PixelUi.OUTLINE, 3));

        hero = findViewById(R.id.menuHero);
        Fx.bob(hero);

        tvGold = findViewById(R.id.tvGold);
        tvLevel = findViewById(R.id.tvLevel);
        tvExp = findViewById(R.id.tvExp);
        tvHp = findViewById(R.id.tvHp);
        tvDamage = findViewById(R.id.tvDamage);
        tvPushups = findViewById(R.id.tvPushups);
        tvPlankTime = findViewById(R.id.tvPlankTime);
        barExp = findViewById(R.id.barExp);
        barHp = findViewById(R.id.barHp);
        barExp.setFillColor(0xFF38BDF8);
        barHp.setFillColor(0xFFF43F5E);
        TextView btnTrain = findViewById(R.id.btnTrain);
        TextView btnBattle = findViewById(R.id.btnBattle);
        TextView btnShop = findViewById(R.id.btnShop);
        TextView btnGacha = findViewById(R.id.btnGacha);
        TextView btnInv = findViewById(R.id.btnInventory);
        btnTrain.setBackground(PixelUi.button(this, 0xFF10B981));
        btnBattle.setBackground(PixelUi.button(this, PixelUi.AMBER));
        btnShop.setBackground(PixelUi.button(this, 0xFF0EA5E9));
        btnGacha.setBackground(PixelUi.button(this, 0xFF9333EA));
        btnInv.setBackground(PixelUi.button(this, 0xFF28314B));

        btnTrain.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            showTrainingPicker();
        });
        btnBattle.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            startActivity(new Intent(this, MapActivity.class));
        });
        btnShop.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            startActivity(new Intent(this, ShopActivity.class));
        });
        btnGacha.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            startActivity(new Intent(this, GachaActivity.class));
        });
        btnInv.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            startActivity(new Intent(this, InventoryActivity.class));
        });

        PixelUi.applyFont(findViewById(android.R.id.content));
    }

    /** Chọn bài tập: Hít đất (mỗi rep = EXP) hoặc Plank (giữ chuẩn 3 giây = 1 EXP + vé Gacha hằng ngày). */
    private void showTrainingPicker() {
        final PixelDialog[] dlg = new PixelDialog[1];
        PixelDialog d = new PixelDialog(this).title("CHỌN BÀI TẬP", PixelUi.AMBER_LIGHT);
        PixelDialog.Option push = d.option("HÍT ĐẤT",
                "Mỗi rep hoàn chỉnh = 1 EXP", false, () -> {
                    dlg[0].dismiss();
                    UiUtil.askRotateThen(this, () -> startActivity(new Intent(this, TrainingActivity.class)));
                });
        PixelDialog.Option plank = d.option("PLANK",
                "Giữ lưng thẳng 165°–180°: 3 giây = 1 EXP. Cộng dồn trong ngày để nhận Vé Gacha (tối đa 10 vé/ngày)",
                false, () -> {
                    dlg[0].dismiss();
                    UiUtil.askRotateThen(this, "thấy rõ vai, hông và cổ chân của bạn",
                            () -> startActivity(new Intent(this, PlankActivity.class)));
                });
        d.add(push.row).add(plank.row).button("HỦY", 0xFF2A334D, PixelUi.TEXT, null);
        dlg[0] = d;
        d.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicPlayer.play(this, MusicPlayer.MENU);
        refreshStats();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicPlayer.pause();
    }

    private static String mmss(long ms) {
        long s = ms / 1000;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    private void refreshStats() {
        PlayerData p = PlayerData.load(this); // load đã tự reset số liệu hôm nay nếu sang ngày mới
        hero.setHero(PixelArt.POSE_ATTACK, GameData.ARMORS[p.armorId], GameData.WEAPONS[p.weaponId]);

        tvGold.setText(String.valueOf(p.gold));
        tvLevel.setText("LV." + p.level);
        tvExp.setText(p.exp + "/" + p.expToNext());
        barExp.setFraction(p.exp / (float) p.expToNext(), false);

        tvHp.setText(p.maxHp() + "/" + p.maxHp());
        barHp.setFraction(1f, false);
        tvDamage.setText("SÁT THƯƠNG: " + p.calcDamage(1) + " / rep");

        tvPushups.setText(p.pushupToday + " rep");
        tvPlankTime.setText(mmss(p.plankDailyMs));
    }
}
