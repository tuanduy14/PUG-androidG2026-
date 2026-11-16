package com.example.pugcore;

import android.graphics.Color;

/** Dữ liệu game (port từ src/data/gameData.ts). */
public final class GameData {
    private GameData() {}

    public enum Archetype { SLIME, GOBLIN, SKELETON, ORC, DRAGON }

    /** Phẩm chất vật phẩm (chỉ để phân loại/hiển thị, không cộng thêm chỉ số). */
    public enum Rarity {
        COMMON("THƯỜNG", 0xFF94A3B8),
        RARE("HIẾM", 0xFF38BDF8),
        EPIC("SỬ THI", 0xFFC084FC),
        LEGENDARY("HUYỀN THOẠI", 0xFFFBBF24);

        public final String label;
        public final int color;

        Rarity(String label, int color) { this.label = label; this.color = color; }
    }

    // ====== NGƯỠNG PHẨM CHẤT (cùng chỉ số => luôn cùng phẩm chất) ======
    // Vũ khí xét theo sát thương cộng thêm mỗi rep; giáp xét theo máu cộng thêm.
    // Chỉ số < ngưỡng HIẾM là THƯỜNG. Muốn đổi cách phân loại chỉ cần sửa 6 số này.
    public static final int WEAPON_RARE_MIN = 10, WEAPON_EPIC_MIN = 20, WEAPON_LEGENDARY_MIN = 38;
    public static final int ARMOR_RARE_MIN = 100, ARMOR_EPIC_MIN = 200, ARMOR_LEGENDARY_MIN = 360;

    public static Rarity weaponRarity(int bonusDmg) {
        if (bonusDmg >= WEAPON_LEGENDARY_MIN) return Rarity.LEGENDARY;
        if (bonusDmg >= WEAPON_EPIC_MIN) return Rarity.EPIC;
        if (bonusDmg >= WEAPON_RARE_MIN) return Rarity.RARE;
        return Rarity.COMMON;
    }

    public static Rarity armorRarity(int bonusHp) {
        if (bonusHp >= ARMOR_LEGENDARY_MIN) return Rarity.LEGENDARY;
        if (bonusHp >= ARMOR_EPIC_MIN) return Rarity.EPIC;
        if (bonusHp >= ARMOR_RARE_MIN) return Rarity.RARE;
        return Rarity.COMMON;
    }

    public static final class Weapon {
        public final String id, name, description; public final int damage, price, blade, hilt, bonusDmg;
        public final Rarity rarity;
        /** true = chỉ nhận được từ Gacha, không bán trong cửa hàng. */
        public final boolean gachaOnly;
        Weapon(String id, String name, int damage, int price, String d, String blade, String hilt) {
            this(id, name, damage, price, d, blade, hilt, false);
        }
        Weapon(String id, String name, int damage, int price, String d, String blade, String hilt, boolean gachaOnly) {
            this.gachaOnly = gachaOnly;
            this.id = id; this.name = name; this.damage = damage; this.bonusDmg = id.equals("fist") ? 0 : damage; this.price = price; this.description = d;
            this.rarity = weaponRarity(this.bonusDmg);
            this.blade = Color.parseColor(blade); this.hilt = Color.parseColor(hilt);
        }
    }

    public static final class Armor {
        public final String id, name, description; public final int maxHpBonus, bonusHp, price, primary, accent;
        public final Rarity rarity;
        /** true = chỉ nhận được từ Gacha, không bán trong cửa hàng. */
        public final boolean gachaOnly;
        Armor(String id, String name, int hp, int price, String d, String primary, String accent) {
            this(id, name, hp, price, d, primary, accent, false);
        }
        Armor(String id, String name, int hp, int price, String d, String primary, String accent, boolean gachaOnly) {
            this.gachaOnly = gachaOnly;
            this.id = id; this.name = name; this.maxHpBonus = hp; this.bonusHp = hp; this.price = price; this.description = d;
            this.rarity = armorRarity(this.bonusHp);
            this.primary = Color.parseColor(primary); this.accent = Color.parseColor(accent);
        }
    }

    public static final class Consumable {
        public final String id, name, description; public final int healAmount, price, color;
        Consumable(String id, String name, int heal, int price, String d, String color) {
            this.id = id; this.name = name; this.healAmount = heal; this.price = price; this.description = d;
            this.color = Color.parseColor(color);
        }
    }

    public static class Stage {
        public final int stage, maxHp, atk, attack, goldReward, rewardGold, expReward; public final boolean isBoss;
        public final Archetype archetype; public final String name, tagline;
        Stage(int stage, boolean boss, Archetype a, String name, int hp, int atk, int gold, int exp, String tagline) {
            this.stage = stage; this.isBoss = boss; this.archetype = a; this.name = name;
            this.maxHp = Math.max(1, Math.round(hp * MONSTER_HP_SCALE));
            this.atk = atk; this.attack = atk; this.goldReward = gold; this.rewardGold = gold; this.expReward = exp; this.tagline = tagline;
        }
    }

    public static final class Monster extends Stage {
        Monster(int stage, boolean boss, Archetype a, String name, int hp, int atk, int gold, int exp, String tagline) {
            super(stage, boss, a, name, hp, atk, gold, exp, tagline);
        }
    }

    // ====== CÂN BẰNG GAME (chỉnh ở đây) ======
    /** Thời gian 1 lượt tấn công. Bản mẫu là 3s (click), hít đất thật chậm hơn nhiều nên để 20s. */
    public static final long RUSH_DURATION_MS = 20_000;
    public static final int TURN_SECONDS = 20;
    public static final int POTION_HEAL = 50;
    public static final int POTION_PRICE = 20;
    public static final int BASE_HP = 100;
    /**
     * Bản mẫu tính theo click (~10 click/giây), còn hít đất ~0,5 rep/giây => HP quái gốc quá cao.
     * Toàn bộ HP quái được nhân với hệ số này. Muốn khó như bản mẫu thì đặt 1f.
     */
    public static final float MONSTER_HP_SCALE = 0.25f;
    public static final int BASE_HERO_HP = 100;
    public static final int TOTAL_STAGES = 10;

    public static final Weapon[] WEAPONS = {
            new Weapon("fist", "Găng Vải Tập Sự", 1, 0, "Vũ khí khởi đầu cho tân binh.", "#d9b99b", "#8d6e63"),
            new Weapon("dagger", "Dao Găm Thép", 5, 30, "Nhanh nhẹn, sát thương mỗi rep +5.", "#cbd5e1", "#b45309"),
            new Weapon("sword", "Kiếm Sắt Kỵ Sĩ", 10, 90, "Cân bằng hoàn hảo, mỗi rep 10 DMG.", "#e2e8f0", "#eab308"),
            new Weapon("axe", "Rìu Chiến Lửa", 20, 220, "Uy lực phá giáp, mỗi rep 20 DMG.", "#fb923c", "#7c2d12"),
            new Weapon("holy_blade", "Thánh Kiếm Ánh Sáng", 38, 500, "Thần binh tối thượng, mỗi rep 38 DMG.", "#38bdf8", "#facc15"),
            // ---- GACHA: THƯỜNG (3) ----
            new Weapon("g_oak_staff", "Gậy Gỗ Sồi", 3, 0, "Chắc tay, +3 sát thương mỗi rep. Chỉ có từ Gacha.", "#a16207", "#78350f", true),
            new Weapon("g_hunter_knife", "Dao Săn Cũ", 6, 0, "Cũ nhưng còn sắc, +6 sát thương mỗi rep. Chỉ có từ Gacha.", "#a1a1aa", "#57534e", true),
            new Weapon("g_smith_hammer", "Búa Thợ Rèn", 8, 0, "Nặng tay, +8 sát thương mỗi rep. Chỉ có từ Gacha.", "#9ca3af", "#92400e", true),
            // ---- GACHA: HIẾM (3) ----
            new Weapon("g_traveler_sword", "Kiếm Bạc Lữ Hành", 12, 0, "Lưỡi bạc sáng loáng, +12 sát thương mỗi rep. Chỉ có từ Gacha.", "#e5e7eb", "#3b82f6", true),
            new Weapon("g_north_spear", "Giáo Gió Bắc", 14, 0, "Nhẹ như gió, +14 sát thương mỗi rep. Chỉ có từ Gacha.", "#bae6fd", "#0e7490", true),
            new Weapon("g_spike_mace", "Chùy Gai Sắt", 16, 0, "Gai nhọn khó đỡ, +16 sát thương mỗi rep. Chỉ có từ Gacha.", "#94a3b8", "#1e3a8a", true),
            // ---- GACHA: SỬ THI (4) ----
            new Weapon("g_hell_blade", "Kiếm Hỏa Ngục", 23, 0, "Rực lửa địa ngục, +23 sát thương mỗi rep. Chỉ có từ Gacha.", "#f97316", "#7f1d1d", true),
            new Weapon("g_shadow_twin", "Song Đao Bóng Đêm", 27, 0, "Ẩn mình trong bóng tối, +27 sát thương mỗi rep. Chỉ có từ Gacha.", "#a78bfa", "#1e1b4b", true),
            new Weapon("g_thunder_axe", "Rìu Sấm Sét", 31, 0, "Chẻ đôi cả tia chớp, +31 sát thương mỗi rep. Chỉ có từ Gacha.", "#fde047", "#4c1d95", true),
            new Weapon("g_frost_lance", "Trường Thương Băng Giá", 35, 0, "Lạnh thấu xương, +35 sát thương mỗi rep. Chỉ có từ Gacha.", "#67e8f9", "#155e75", true),
            // ---- GACHA: HUYỀN THOẠI (2) ----
            new Weapon("g_dragon_fang", "Long Nha Kiếm", 45, 0, "Rèn từ răng rồng cổ đại, +45 sát thương mỗi rep. Chỉ có từ Gacha.", "#f87171", "#fbbf24", true),
            new Weapon("g_world_blade", "Thiên Kiếm Chấn Thế", 55, 0, "Một nhát rung chuyển thế gian, +55 sát thương mỗi rep. Chỉ có từ Gacha.", "#fef08a", "#f59e0b", true),
    };

    public static final Armor[] ARMORS = {
            new Armor("cloth", "Áo Tập Võ", 0, 0, "Trang phục cơ bản, nhẹ và thoáng.", "#3b82f6", "#1d4ed8"),
            new Armor("leather", "Giáp Da Săn Quái", 40, 45, "Tăng +40 HP tối đa cho hiệp sĩ.", "#a16207", "#713f12"),
            new Armor("iron", "Giáp Sắt Hoàng Gia", 100, 130, "Tăng +100 HP tối đa, phòng ngự vững chắc.", "#94a3b8", "#475569"),
            new Armor("knight", "Giáp Vàng Thánh Điện", 200, 300, "Tăng +200 HP tối đa, khí chất bất khuất.", "#eab308", "#a16207"),
            new Armor("dragon", "Giáp Vảy Rồng Đỏ", 360, 620, "Tăng +360 HP tối đa, rèn từ vảy rồng.", "#dc2626", "#7f1d1d"),
            // ---- GACHA: THƯỜNG (3) ----
            new Armor("g_rough_cloth", "Áo Vải Thô Dày", 20, 0, "Dày dặn, +20 HP tối đa. Chỉ có từ Gacha.", "#a8a29e", "#78716c", true),
            new Armor("g_wolf_leather", "Giáp Da Sói", 60, 0, "Lột từ sói rừng, +60 HP tối đa. Chỉ có từ Gacha.", "#78716c", "#44403c",




                    true),
            new Armor("g_wood_plate", "Áo Giáp Tấm Gỗ", 80, 0, "Ghép từ gỗ cứng, +80 HP tối đa. Chỉ có từ Gacha.", "#a16207", "#78350f", true),
            // ---- GACHA: HIẾM (3) ----
            new Armor("g_chain_legion", "Giáp Xích Lữ Đoàn", 120, 0, "Của lính đánh thuê, +120 HP tối đa. Chỉ có từ Gacha.", "#9ca3af", "#4b5563", true),
            new Armor("g_blue_jade", "Giáp Lam Ngọc", 145, 0, "Khảm ngọc xanh, +145 HP tối đa. Chỉ có từ Gacha.", "#38bdf8", "#0369a1", true),
            new Armor("g_north_steel", "Giáp Thép Phương Bắc", 170, 0, "Rèn trong giá lạnh, +170 HP tối đa. Chỉ có từ Gacha.", "#cbd5e1", "#334155", true),
            // ---- GACHA: SỬ THI (4) ----
            new Armor("g_dark_knight", "Giáp Hắc Kỵ Sĩ", 240, 0, "Của kỵ sĩ bóng đêm, +240 HP tối đa. Chỉ có từ Gacha.", "#4b5563", "#111827", true),
            new Armor("g_thunder_mail", "Chiến Giáp Lôi Điện", 270, 0, "Tích điện sau mỗi trận, +270 HP tối đa. Chỉ có từ Gacha.", "#facc15", "#1d4ed8", true),
            new Armor("g_ice_crystal", "Giáp Băng Tinh", 300, 0, "Kết từ tinh thể băng, +300 HP tối đa. Chỉ có từ Gacha.", "#a5f3fc", "#0e7490", true),
            new Armor("g_amber_ancient", "Giáp Hổ Phách Cổ", 330, 0, "Phong ấn sức mạnh ngàn năm, +330 HP tối đa. Chỉ có từ Gacha.", "#f59e0b", "#92400e", true),
            // ---- GACHA: HUYỀN THOẠI (2) ----
            new Armor("g_dragon_scale", "Giáp Long Lân Bất Diệt", 420, 0, "Vảy rồng xanh không thể phá, +420 HP tối đa. Chỉ có từ Gacha.", "#10b981", "#064e3b", true),
            new Armor("g_angel_armor", "Thánh Giáp Thiên Sứ", 500, 0, "Ánh sáng che chở, +500 HP tối đa. Chỉ có từ Gacha.", "#fef3c7", "#f59e0b", true),
    };

    public static final Consumable[] CONSUMABLES = {
            new Consumable("potion_small", "Bình Máu Nhỏ", 50, 20, "Hồi ngay 50 HP khi đang chiến đấu.", "#ef4444"),
            new Consumable("potion_large", "Bình Máu Đại", 140, 50, "Hồi ngay 140 HP khi đang chiến đấu.", "#f43f5e"),
    };

    public static final Monster[] MONSTERS = {
            new Monster(1, false, Archetype.SLIME, "Slime Xanh Nhớt", 40, 7, 25, 8, "Quái tập sự ở bìa rừng"),
            new Monster(2, false, Archetype.SLIME, "Slime Gai Rừng", 75, 10, 36, 12, "Slime đột biến hung hãn hơn"),
            new Monster(3, false, Archetype.GOBLIN, "Yêu Tinh Goblin", 120, 14, 50, 16, "Kẻ cướp ranh mãnh cầm dao găm"),
            new Monster(4, false, Archetype.GOBLIN, "Goblin Đạo Tặc", 175, 19, 70, 22, "Tên trinh sát canh cổng doanh trại"),
            new Monster(5, true, Archetype.ORC, "Thủ Lĩnh Orc Đỏ", 300, 26, 145, 48, "BOSS VÁN 5 · Bạo chúa rừng sâu"),
            new Monster(6, false, Archetype.SKELETON, "Bộ Xương Kiếm Sĩ", 360, 31, 115, 38, "Chiến binh cổ đại trong hầm mộ"),
            new Monster(7, false, Archetype.SKELETON, "Kỵ Sĩ Xương Đen", 460, 37, 150, 50, "Hộ vệ bất tử với thanh kiếm nguyền"),
            new Monster(8, false, Archetype.ORC, "Orc Thiết Giáp", 600, 45, 195, 65, "Cận vệ tinh nhuệ của Ma Vương"),
            new Monster(9, false, Archetype.SKELETON, "Tử Thần Hầm Ngục", 780, 54, 250, 82, "Cửa ngõ cuối trước hang Rồng"),
            new Monster(10, true, Archetype.DRAGON, "Rồng Lửa Cổ Đại", 1150, 68, 500, 160, "BOSS CUỐI VÁN 10 · Chúa tể dung nham"),
    };

    public static final Stage[] STAGES = MONSTERS;

    public static int expToNext(int level) {
        return (int) Math.floor(10 * Math.pow(1.35, level - 1));
    }

    /** Hệ số sát thương theo cấp: 1 rep ở Lv2 = 1.1 rep ở Lv1. */
    public static double levelMultiplier(int level) {
        return Math.round((1 + (level - 1) * 0.1) * 10) / 10.0;
    }

    public static Stage stage(int n) {
        return STAGES[Math.max(1, Math.min(TOTAL_STAGES, n)) - 1];
    }

    /** Vũ khí chỉ có từ Gacha theo phẩm chất (dùng làm kho phần thưởng). */
    public static java.util.List<Weapon> gachaWeapons(Rarity r) {
        java.util.List<Weapon> out = new java.util.ArrayList<>();
        for (Weapon w : WEAPONS) if (w.gachaOnly && w.rarity == r) out.add(w);
        return out;
    }

    /** Giáp chỉ có từ Gacha theo phẩm chất. */
    public static java.util.List<Armor> gachaArmors(Rarity r) {
        java.util.List<Armor> out = new java.util.ArrayList<>();
        for (Armor a : ARMORS) if (a.gachaOnly && a.rarity == r) out.add(a);
        return out;
    }

    public static Weapon weapon(String id) {
        for (Weapon w : WEAPONS) if (w.id.equals(id)) return w;
        return WEAPONS[0];
    }

    public static Armor armor(String id) {
        for (Armor a : ARMORS) if (a.id.equals(id)) return a;
        return ARMORS[0];
    }
}