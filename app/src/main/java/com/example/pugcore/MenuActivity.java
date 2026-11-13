package com.example.pugcore;

import android.content.Intent;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MenuActivity extends AppCompatActivity {

    private TextView tvStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        UiUtil.applySystemBarPadding(findViewById(R.id.root));

        tvStats = findViewById(R.id.tvStats);
        findViewById(R.id.btnTrain).setOnClickListener(v ->
                askRotateThen(() -> startActivity(new Intent(this, TrainingActivity.class))));
        findViewById(R.id.btnBattle).setOnClickListener(v -> showMonsterPicker());
        findViewById(R.id.btnShop).setOnClickListener(v ->
                startActivity(new Intent(this, ShopActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        PlayerData p = PlayerData.load(this);
        GameData.Weapon w = GameData.WEAPONS[p.weaponId];
        GameData.Armor a = GameData.ARMORS[p.armorId];
        tvStats.setText(String.format(Locale.getDefault(),
                "Cấp %d  (1 rep = %.1f rep ở cấp 1)\nEXP: %d/%d\nVàng: %d\n"
                        + "Vũ khí: %s (+%d sát thương/rep)\nGiáp: %s (máu tối đa %d)\nBình máu: %d",
                p.level, p.repScale(), p.exp, p.expToNext(), p.gold,
                w.name, w.bonusDmg, a.name, p.maxHp(), p.potions));
    }

    /**
     * Tập luyện và Chiến đấu chạy ở chế độ ngang. Hiện thông báo yêu cầu xoay điện thoại;
     * khi cảm biến thấy máy đã nằm ngang thì tự vào luôn, hoặc bấm "Tiếp tục" để vào ngay.
     */
    private void askRotateThen(Runnable start) {
        final boolean[] started = {false};
        final OrientationEventListener[] listener = new OrientationEventListener[1];

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Xoay ngang điện thoại")
                .setMessage("Hãy xoay điện thoại sang chiều ngang rồi đặt máy sao cho camera "
                        + "thấy rõ vai và cánh tay của bạn.\n\nApp sẽ tự vào khi máy nằm ngang, "
                        + "hoặc bấm Tiếp tục.")
                .create();

        Runnable go = () -> {
            if (started[0]) return;
            started[0] = true;
            if (listener[0] != null) listener[0].disable();
            dialog.dismiss();
            start.run();
        };
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Tiếp tục", (d, w) -> go.run());
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Hủy", (d, w) -> { });
        dialog.setOnDismissListener(d -> {
            if (listener[0] != null) listener[0].disable();
        });

        listener[0] = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int o) {
                if (o == ORIENTATION_UNKNOWN) return;
                if ((o >= 60 && o <= 120) || (o >= 240 && o <= 300)) go.run();
            }
        };
        dialog.show();
        if (listener[0].canDetectOrientation()) listener[0].enable();
    }

    private void showMonsterPicker() {
        String[] items = new String[GameData.MONSTERS.length];
        for (int i = 0; i < items.length; i++) {
            GameData.Monster m = GameData.MONSTERS[i];
            items[i] = m.name + " — HP " + m.maxHp + ", ATK " + m.attack
                    + ", thưởng " + m.rewardGold + " vàng";
        }
        new AlertDialog.Builder(this)
                .setTitle("Chọn quái")
                .setItems(items, (dialog, which) -> askRotateThen(() -> {
                    Intent i = new Intent(this, BattleActivity.class);
                    i.putExtra(BattleActivity.EXTRA_MONSTER, which);
                    startActivity(i);
                }))
                .show();
    }
}
