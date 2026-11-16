package com.example.pugcore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Kho trang bị (dọc): mọi vũ khí/giáp đã sở hữu (cửa hàng + Gacha), chọn món để trang bị. */
public class InventoryActivity extends AppCompatActivity {

    private PlayerData player;
    private LinearLayout container;
    private TextView tvGold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PixelUi.init(this);
        SoundFx.init(this);
        setContentView(R.layout.activity_inventory);
        UiUtil.applySystemBarPadding(findViewById(R.id.root));
        SettingsDialog.attach(this);

        container = findViewById(R.id.container);
        tvGold = findViewById(R.id.tvGold);
        findViewById(R.id.goldBox).setBackground(PixelUi.box(this, 0xFF0F1320, PixelUi.OUTLINE, 2));
        TextView back = findViewById(R.id.btnBack);
        back.setBackground(PixelUi.button(this, 0xFF28314B));
        back.setOnClickListener(v -> finish());
        PixelUi.applyFont(findViewById(android.R.id.content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicPlayer.play(this, MusicPlayer.MENU);
        player = PlayerData.load(this);
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicPlayer.pause();
    }

    private void render() {
        container.removeAllViews();
        tvGold.setText(String.valueOf(player.gold));

        addHeader("VŨ KHÍ");
        for (int i = 0; i < GameData.WEAPONS.length; i++) {
            if (!player.ownsWeapon(i)) continue;
            final int id = i;
            GameData.Weapon w = GameData.WEAPONS[i];
            addRow(w.rarity, "sword", w.blade, w.name, "+" + w.bonusDmg + " sát thương mỗi rep", 0xFFFCD34D,
                    w.description, player.weaponId == id, v -> {
                        player.weaponId = id;
                        SoundFx.play(SoundFx.BUY);
                        player.save(this);
                        render();
                    });
        }

        addHeader("GIÁP");
        for (int i = 0; i < GameData.ARMORS.length; i++) {
            if (!player.ownsArmor(i)) continue;
            final int id = i;
            GameData.Armor a = GameData.ARMORS[i];
            addRow(a.rarity, "shield", a.primary, a.name, "+" + a.bonusHp + " máu tối đa", 0xFFFDA4AF,
                    a.description, player.armorId == id, v -> {
                        player.armorId = id;
                        SoundFx.play(SoundFx.BUY);
                        player.save(this);
                        render();
                    });
        }
    }

    private void addHeader(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(PixelUi.AMBER_LIGHT);
        tv.setTypeface(PixelUi.font);
        tv.setPadding(0, (int) PixelUi.dp(this, 14), 0, (int) PixelUi.dp(this, 6));
        container.addView(tv);
    }

    private void addRow(GameData.Rarity rarity, String icon, int tint, String name, String stat, int statColor,
                        String desc, boolean equipped, View.OnClickListener onEquip) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_shop_row, container, false);
        row.setBackground(PixelUi.box(this, 0xFF1C2233, PixelUi.OUTLINE, 3));
        row.findViewById(R.id.rowIconBox).setBackground(PixelUi.box(this, 0xFF101420, rarity.color, 2));
        TextView tag = row.findViewById(R.id.rowRarity);
        tag.setText("[" + rarity.label + "]");
        tag.setTextColor(rarity.color);
        ((PixelSpriteView) row.findViewById(R.id.rowIcon)).setIcon(icon, tint);
        TextView nameView = row.findViewById(R.id.rowName);
        nameView.setText(name);
        nameView.setTextColor(rarity.color);
        TextView st = row.findViewById(R.id.rowStat);
        st.setText(stat);
        st.setTextColor(statColor);
        ((TextView) row.findViewById(R.id.rowDesc)).setText(desc);

        TextView btn = row.findViewById(R.id.rowBtn);
        if (equipped) {
            btn.setText("Đang dùng");
            btn.setBackground(PixelUi.button(this, 0xFF047857));
            btn.setTextColor(0xFFF1F5F9);
            PixelUi.setEnabledLook(btn, false);
        } else {
            btn.setText("Trang bị");
            btn.setBackground(PixelUi.button(this, 0xFF0EA5E9));
            btn.setTextColor(PixelUi.DARK_TEXT);
            btn.setOnClickListener(onEquip);
        }
        PixelUi.applyFont(row);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
        lp.bottomMargin = (int) PixelUi.dp(this, 10);
        container.addView(row, lp);
    }
}
