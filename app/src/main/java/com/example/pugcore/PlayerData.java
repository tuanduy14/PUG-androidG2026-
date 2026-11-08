package com.example.pugcore;

import android.content.Context;
import android.content.SharedPreferences;

/** Dữ liệu nhân vật, lưu bằng SharedPreferences. */
public class PlayerData {
    private static final String PREFS = "pug_save";

    public int level = 1;
    public int exp = 0;
    public int gold = 0;
    public int weaponId = 0; // index trong GameData.WEAPONS
    public int armorId = 0;  // index trong GameData.ARMORS
    public int potions = 0;

    public static PlayerData load(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        PlayerData p = new PlayerData();
        p.level = sp.getInt("level", 1);
        p.exp = sp.getInt("exp", 0);
        p.gold = sp.getInt("gold", 0);
        p.weaponId = sp.getInt("weaponId", 0);
        p.armorId = sp.getInt("armorId", 0);
        p.potions = sp.getInt("potions", 0);
        return p;
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("level", level)
                .putInt("exp", exp)
                .putInt("gold", gold)
                .putInt("weaponId", weaponId)
                .putInt("armorId", armorId)
                .putInt("potions", potions)
                .apply();
    }

    /** EXP cần để lên cấp tiếp theo. */
    public int expToNext() {
        return 20 * level;
    }

    /** 1 rep ở cấp n = (1 + 0.1*(n-1)) rep ở cấp 1. */
    public double repScale() {
        return 1.0 + 0.1 * (level - 1);
    }

    /** Sát thương mỗi rep: 1 cơ bản + thưởng của vũ khí. */
    public int damagePerRep() {
        return 1 + GameData.WEAPONS[weaponId].bonusDmg;
    }

    public int maxHp() {
        return GameData.BASE_HP + GameData.ARMORS[armorId].bonusHp;
    }

    public int calcDamage(int reps) {
        return (int) Math.round(reps * repScale() * damagePerRep());
    }

    /** Cộng EXP, trả về số cấp vừa tăng. */
    public int addExp(int amount) {
        exp += amount;
        int gained = 0;
        while (exp >= expToNext()) {
            exp -= expToNext();
            level++;
            gained++;
        }
        return gained;
    }
}
