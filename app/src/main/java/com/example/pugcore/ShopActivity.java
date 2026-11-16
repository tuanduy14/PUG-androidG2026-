package com.example.pugcore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Cửa hàng (dọc). Mua một lần là sở hữu; món đã có thì chọn "Trang bị".
 * Trang bị chỉ có từ Gacha (gachaOnly) không bán ở đây — xem/trang bị trong Kho đồ.
 */
public class ShopActivity extends AppCompatActivity {

    private PlayerData player;
    private LinearLayout container;
    private TextView tvGold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PixelUi.init(this);
        SoundFx.init(this);
        setContentView(R.layout.activity_shop);
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
        for (int i = 1; i < GameData.WEAPONS.length; i++) {
            final int id = i;
            GameData.Weapon w = GameData.WEAPONS[i];
            if (w.gachaOnly) continue; // chỉ có từ Gacha
            boolean owned = player.ownsWeapon(id), equipped = player.weaponId == id;
            addRow(w.rarity, "sword", w.blade, w.name, "+" + w.bonusDmg + " sát thương mỗi rep", 0xFFFCD34D,
                    w.description, w.price, owned, equipped, v -> {
                        if (player.ownsWeapon(id)) { player.weaponId = id; SoundFx.play(SoundFx.BUY); player.save(this); render(); return; }
                        if (!spend(w.price)) return;
                        player.ownedWeapons.add(id);
                        player.weaponId = id;
                        SoundFx.play(SoundFx.BUY);
                        player.save(this);
                        render();
                    });
        }

        addHeader("GIÁP");
        for (int i = 1; i < GameData.ARMORS.length; i++) {
            final int id = i;
            GameData.Armor a = GameData.ARMORS[i];
            if (a.gachaOnly) continue; // chỉ có từ Gacha
            boolean owned = player.ownsArmor(id), equipped = player.armorId == id;
            addRow(a.rarity, "shield", a.primary, a.name, "+" + a.bonusHp + " máu tối đa", 0xFFFDA4AF,
                    a.description, a.price, owned, equipped, v -> {
                        if (player.ownsArmor(id)) { player.armorId = id; SoundFx.play(SoundFx.BUY); player.save(this); render(); return; }
                        if (!spend(a.price)) return;
                        player.ownedArmors.add(id);
                        player.armorId = id;
                        SoundFx.play(SoundFx.BUY);
                        player.save(this);
                        render();
                    });
        }

        addHeader("VẬT PHẨM");
        addRow(null, "potion", 0xFFEF4444, "Bình máu", "Hồi " + GameData.POTION_HEAL + " máu khi chiến đấu", 0xFF6EE7B7,
                "Đang có: " + player.potions, GameData.POTION_PRICE, false, false, v -> {
                    if (!spend(GameData.POTION_PRICE)) return;
                    player.potions++;
                    SoundFx.play(SoundFx.BUY);
                    player.save(this);
                    render();
                });
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

    /** owned/equipped quyết định nút: Đang dùng (khoá) / Trang bị / Mua (giá). */
    private void addRow(GameData.Rarity rarity, String icon, int tint, String name, String stat, int statColor,
                        String desc, int price, boolean owned, boolean equipped, View.OnClickListener onClick) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_shop_row, container, false);
        row.setBackground(PixelUi.box(this, 0xFF1C2233, PixelUi.OUTLINE, 3));
        row.findViewById(R.id.rowIconBox).setBackground(PixelUi.box(this, 0xFF101420,
                rarity == null ? PixelUi.OUTLINE : rarity.color, 2));
        TextView tag = row.findViewById(R.id.rowRarity);
        if (rarity == null) {
            tag.setVisibility(View.GONE);
        } else {
            tag.setText("[" + rarity.label + "]");
            tag.setTextColor(rarity.color);
        }
        ((PixelSpriteView) row.findViewById(R.id.rowIcon)).setIcon(icon, tint);
        TextView nameView = row.findViewById(R.id.rowName);
        nameView.setText(name);
        if (rarity != null) nameView.setTextColor(rarity.color);
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
        } else if (owned) {
            btn.setText("Trang bị");
            btn.setBackground(PixelUi.button(this, 0xFF0EA5E9));
            btn.setTextColor(PixelUi.DARK_TEXT);
            btn.setOnClickListener(onClick);
        } else {
            boolean afford = player.gold >= price;
            btn.setText("Mua (" + price + ")");
            btn.setBackground(PixelUi.button(this, afford ? PixelUi.AMBER : 0xFF334155));
            btn.setTextColor(afford ? PixelUi.DARK_TEXT : 0xFFCBD5E1);
            btn.setOnClickListener(onClick);
        }
        PixelUi.applyFont(row);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) row.getLayoutParams();
        lp.bottomMargin = (int) PixelUi.dp(this, 10);
        container.addView(row, lp);
    }

    private boolean spend(int price) {
        if (player.gold < price) {
            Toast.makeText(this, "Không đủ vàng", Toast.LENGTH_SHORT).show();
            return false;
        }
        player.gold -= price;
        return true;
    }
}
