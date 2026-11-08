package com.example.pugcore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ShopActivity extends AppCompatActivity {

    private PlayerData player;
    private LinearLayout container;
    private TextView tvGold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        UiUtil.applySystemBarPadding(findViewById(R.id.root));

        container = findViewById(R.id.container);
        tvGold = findViewById(R.id.tvGold);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        player = PlayerData.load(this);
        render();
    }

    private void render() {
        container.removeAllViews();
        tvGold.setText("Vàng: " + player.gold);

        addHeader("Vũ khí");
        for (int i = 1; i < GameData.WEAPONS.length; i++) {
            final int id = i;
            GameData.Weapon w = GameData.WEAPONS[i];
            addRow(w.name, "+" + w.bonusDmg + " sát thương mỗi rep", w.price,
                    id == player.weaponId ? "Đang dùng" : "Đã có",
                    id > player.weaponId, v -> buyWeapon(id));
        }

        addHeader("Giáp");
        for (int i = 1; i < GameData.ARMORS.length; i++) {
            final int id = i;
            GameData.Armor a = GameData.ARMORS[i];
            addRow(a.name, "+" + a.bonusHp + " máu tối đa", a.price,
                    id == player.armorId ? "Đang dùng" : "Đã có",
                    id > player.armorId, v -> buyArmor(id));
        }

        addHeader("Vật phẩm");
        addRow("Bình máu", "Hồi " + GameData.POTION_HEAL + " máu khi chiến đấu (đang có "
                        + player.potions + ")", GameData.POTION_PRICE,
                "Mua", true, v -> buyPotion());
    }

    private void addHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(20);
        tv.setPadding(0, 32, 0, 8);
        container.addView(tv);
    }

    private void addRow(String name, String desc, int price, String disabledLabel,
                        boolean canBuy, View.OnClickListener onBuy) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_shop, container, false);
        ((TextView) row.findViewById(R.id.tvName)).setText(name);
        ((TextView) row.findViewById(R.id.tvDesc)).setText(desc);
        Button btn = row.findViewById(R.id.btnBuy);
        if (canBuy) {
            btn.setText("Mua (" + price + ")");
            btn.setOnClickListener(onBuy);
        } else {
            btn.setText(disabledLabel);
            btn.setEnabled(false);
        }
        container.addView(row);
    }

    private boolean spend(int price) {
        if (player.gold < price) {
            Toast.makeText(this, "Không đủ vàng", Toast.LENGTH_SHORT).show();
            return false;
        }
        player.gold -= price;
        return true;
    }

    private void buyWeapon(int id) {
        if (id <= player.weaponId || !spend(GameData.WEAPONS[id].price)) return;
        player.weaponId = id;
        player.save(this);
        render();
    }

    private void buyArmor(int id) {
        if (id <= player.armorId || !spend(GameData.ARMORS[id].price)) return;
        player.armorId = id;
        player.save(this);
        render();
    }

    private void buyPotion() {
        if (!spend(GameData.POTION_PRICE)) return;
        player.potions++;
        player.save(this);
        render();
    }
}
