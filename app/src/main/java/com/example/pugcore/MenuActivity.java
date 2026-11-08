package com.example.pugcore;

import android.content.Intent;
import android.os.Bundle;
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
                startActivity(new Intent(this, TrainingActivity.class)));
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

    private void showMonsterPicker() {
        String[] items = new String[GameData.MONSTERS.length];
        for (int i = 0; i < items.length; i++) {
            GameData.Monster m = GameData.MONSTERS[i];
            items[i] = m.name + " — HP " + m.maxHp + ", ATK " + m.attack
                    + ", thưởng " + m.rewardGold + " vàng";
        }
        new AlertDialog.Builder(this)
                .setTitle("Chọn quái")
                .setItems(items, (dialog, which) -> {
                    Intent i = new Intent(this, BattleActivity.class);
                    i.putExtra(BattleActivity.EXTRA_MONSTER, which);
                    startActivity(i);
                })
                .show();
    }
}
