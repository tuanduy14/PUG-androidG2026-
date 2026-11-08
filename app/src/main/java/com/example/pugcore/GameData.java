package com.example.pugcore;

/** Toàn bộ số liệu cân bằng game nằm ở đây, muốn chỉnh chỉ cần sửa file này. */
public final class GameData {
    private GameData() {}

    public static final int BASE_HP = 100;
    public static final int POTION_PRICE = 50;
    public static final int POTION_HEAL = 50;
    public static final int TURN_SECONDS = 20; // thời gian tối đa của 1 lượt đánh

    public static class Weapon {
        public final String name;
        public final int bonusDmg; // sát thương cộng thêm mỗi rep
        public final int price;

        Weapon(String name, int bonusDmg, int price) {
            this.name = name;
            this.bonusDmg = bonusDmg;
            this.price = price;
        }
    }

    public static class Armor {
        public final String name;
        public final int bonusHp;
        public final int price;

        Armor(String name, int bonusHp, int price) {
            this.name = name;
            this.bonusHp = bonusHp;
            this.price = price;
        }
    }

    public static class Monster {
        public final String name;
        public final int maxHp;
        public final int attack; // sát thương mỗi lần phản công
        public final int rewardGold;

        Monster(String name, int maxHp, int attack, int rewardGold) {
            this.name = name;
            this.maxHp = maxHp;
            this.attack = attack;
            this.rewardGold = rewardGold;
        }
    }

    // Index 0 = trang bị mặc định (không bán trong shop)
    public static final Weapon[] WEAPONS = {
            new Weapon("Tay không", 0, 0),
            new Weapon("Dao", 5, 100),
            new Weapon("Kiếm", 10, 300),
            new Weapon("Rìu chiến", 20, 800)
    };

    public static final Armor[] ARMORS = {
            new Armor("Không giáp", 0, 0),
            new Armor("Giáp da", 50, 150),
            new Armor("Giáp sắt", 120, 400),
            new Armor("Giáp thép", 250, 900)
    };

    public static final Monster[] MONSTERS = {
            new Monster("Slime", 30, 5, 20),
            new Monster("Goblin", 80, 10, 50),
            new Monster("Orc", 200, 20, 120),
            new Monster("Rồng", 500, 40, 400)
    };
}
