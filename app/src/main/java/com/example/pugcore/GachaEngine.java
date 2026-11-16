package com.example.pugcore;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cơ chế quay gacha.
 * Tỉ lệ (tổng 100%): Vàng 33% (2G 16 / 5G 10 / 10G 6 / 100G 1), Thường 40, Hiếm 18.5, Sử thi 7, Huyền thoại 1.5.
 * Bảo hiểm: quay x10 chắc chắn có ≥1 Sử thi trở lên; quay đủ 70 lần chưa ra Huyền thoại thì lần thứ 70 chắc chắn ra.
 * Trang bị chỉ lấy từ nhóm món có cờ gachaOnly (cả vũ khí lẫn giáp cùng phẩm chất chia đều).
 */
public final class GachaEngine {
    private GachaEngine() {}

    public static final int PITY_LEGEND = 70;

    /** Vàng nhận khi quay trúng trang bị TRÙNG (tạm thời): Thường 2 / Hiếm 5 / Sử thi 10 / Huyền thoại 50. */
    public static int duplicateGold(GameData.Rarity r) {
        switch (r) {
            case COMMON: return 2;
            case RARE: return 5;
            case EPIC: return 10;
            default: return 50;
        }
    }

    // đổi tỉ lệ ở đây (đơn vị %, tổng phải bằng 100)
    public static final double RATE_GOLD2 = 16.0, RATE_GOLD5 = 10.0, RATE_GOLD10 = 6.0, RATE_GOLD100 = 1.0;
    public static final double RATE_COMMON = 40.0, RATE_RARE = 18.5, RATE_EPIC = 7.0, RATE_LEGENDARY = 1.5;

    private static final int GOLD2 = 0, GOLD5 = 1, GOLD10 = 2, GOLD100 = 3, COMMON = 4, RARE = 5, EPIC = 6, LEGEND = 7;
    private static final int[] GOLD_VALUE = {2, 5, 10, 100};

    /** Một phần thưởng: hoặc vàng, hoặc 1 vũ khí, hoặc 1 giáp. */
    public static final class Drop {
        public final int gold;                 // > 0 nếu là vàng
        public final GameData.Weapon weapon;   // khác null nếu là vũ khí
        public final GameData.Armor armor;     // khác null nếu là giáp
        public final boolean duplicate;        // đã có sẵn trong kho (trùng)
        public final int refund;               // vàng quy đổi khi trùng (0 nếu không trùng)

        Drop(int gold, GameData.Weapon w, GameData.Armor a, boolean dup, int refund) {
            this.gold = gold; this.weapon = w; this.armor = a; this.duplicate = dup; this.refund = refund;
        }

        public boolean isGear() { return weapon != null || armor != null; }

        /** Phẩm chất của trang bị; null nếu là vàng. */
        public GameData.Rarity rarity() {
            return weapon != null ? weapon.rarity : armor != null ? armor.rarity : null;
        }

        public boolean isEpicPlus() {
            GameData.Rarity r = rarity();
            return r == GameData.Rarity.EPIC || r == GameData.Rarity.LEGENDARY;
        }
    }

    private static final class Gear {
        final boolean weapon; final int index;
        Gear(boolean weapon, int index) { this.weapon = weapon; this.index = index; }
    }

    private static int roll(Random rnd) {
        double x = rnd.nextDouble() * 100.0;
        double[] rates = {RATE_GOLD2, RATE_GOLD5, RATE_GOLD10, RATE_GOLD100, RATE_COMMON, RATE_RARE, RATE_EPIC, RATE_LEGENDARY};
        for (int i = 0; i < rates.length; i++) {
            if (x < rates[i]) return i;
            x -= rates[i];
        }
        return COMMON; // chỉ xảy ra nếu tổng tỉ lệ < 100
    }

    private static GameData.Rarity rarityOf(int tier) {
        switch (tier) {
            case COMMON: return GameData.Rarity.COMMON;
            case RARE: return GameData.Rarity.RARE;
            case EPIC: return GameData.Rarity.EPIC;
            default: return GameData.Rarity.LEGENDARY;
        }
    }

    private static List<Gear> pool(GameData.Rarity r) {
        List<Gear> out = new ArrayList<>();
        for (int i = 0; i < GameData.WEAPONS.length; i++) {
            GameData.Weapon w = GameData.WEAPONS[i];
            if (w.gachaOnly && w.rarity == r) out.add(new Gear(true, i));
        }
        for (int i = 0; i < GameData.ARMORS.length; i++) {
            GameData.Armor a = GameData.ARMORS[i];
            if (a.gachaOnly && a.rarity == r) out.add(new Gear(false, i));
        }
        return out;
    }

    /**
     * Quay `count` lần: cập nhật vàng, kho đồ và bộ đếm bảo hiểm trong `p` (chưa lưu, caller tự save).
     * Vé do caller trừ trước khi gọi.
     */
    public static List<Drop> pull(PlayerData p, int count, Random rnd) {
        List<Drop> out = new ArrayList<>();
        boolean hasEpicPlus = false;
        for (int i = 0; i < count; i++) {
            int tier = roll(rnd);
            if (p.gachaPity >= PITY_LEGEND - 1) {
                tier = LEGEND; // đủ 70 lần không ra Huyền thoại
            } else if (count == 10 && i == count - 1 && !hasEpicPlus && tier != EPIC && tier != LEGEND) {
                // x10 chưa có Sử thi trở lên: lượt cuối được nâng lên Sử thi/Huyền thoại theo tỉ lệ gốc
                tier = rnd.nextDouble() < RATE_EPIC / (RATE_EPIC + RATE_LEGENDARY) ? EPIC : LEGEND;
            }
            if (tier == LEGEND) p.gachaPity = 0; else p.gachaPity++;
            if (tier == EPIC || tier == LEGEND) hasEpicPlus = true;
            out.add(makeDrop(p, tier, rnd));
        }
        return out;
    }

    private static Drop makeDrop(PlayerData p, int tier, Random rnd) {
        if (tier <= GOLD100) {
            int g = GOLD_VALUE[tier];
            p.gold += g;
            return new Drop(g, null, null, false, 0);
        }
        List<Gear> pool = pool(rarityOf(tier));
        Gear g = pool.get(rnd.nextInt(pool.size()));
        if (g.weapon) {
            boolean dup = p.ownedWeapons.contains(g.index);
            p.ownedWeapons.add(g.index);
            int refund = dup ? duplicateGold(GameData.WEAPONS[g.index].rarity) : 0;
            p.gold += refund;
            return new Drop(0, GameData.WEAPONS[g.index], null, dup, refund);
        }
        boolean dup = p.ownedArmors.contains(g.index);
        p.ownedArmors.add(g.index);
        int refund = dup ? duplicateGold(GameData.ARMORS[g.index].rarity) : 0;
        p.gold += refund;
        return new Drop(0, null, GameData.ARMORS[g.index], dup, refund);
    }
}
