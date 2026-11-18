package com.example.pugcore;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Dữ liệu nhân vật, lưu bằng SharedPreferences. */
public class PlayerData {
    private static final String PREFS = "pug_save";

    public int level = 1;
    public int exp = 0;
    public int gold = 0;
    public int weaponId = 0; // index trong GameData.WEAPONS (món đang trang bị)
    public int armorId = 0;  // index trong GameData.ARMORS (món đang trang bị)
    public int potions = 0;
    public int maxStage = 1;     // ván cao nhất đã mở khoá (1..TOTAL_STAGES)
    public int clearedMask = 0;  // bit n = đã thắng ván n

    public static final int MODE_TIMED = 0; // mỗi lượt giới hạn số giây
    public static final int MODE_FREE = 1;  // không giới hạn, 5 giây không có rep thì kết thúc lượt
    public static final int MIN_TURN_SECONDS = 10, MAX_TURN_SECONDS = 30;
    public int turnMode = MODE_FREE;
    public int turnSeconds = GameData.TURN_SECONDS;

    // ---- Kho đồ: các món đã sở hữu (index trong GameData.WEAPONS / ARMORS)
    public final Set<Integer> ownedWeapons = new TreeSet<>();
    public final Set<Integer> ownedArmors = new TreeSet<>();

    // ---- Gacha
    public int gachaTickets = 0;
    public int gachaPity = 0;


    public static final long PLANK_MS_PER_EXP = 3000;
    public static final long[] PLANK_MILESTONE_S = {60, 150, 240, 300};
    public static final int[] PLANK_MILESTONE_TICKETS = {1, 2, 2, 5};
    public static final long PLANK_GOAL_S = 300;
    public String plankDay = "";
    public long plankDailyMs = 0;
    public int plankClaimedMask = 0;
    public long plankRemainderMs = 0;


    public int pushupToday = 0;

    public static PlayerData load(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        PlayerData p = new PlayerData();
        p.level = sp.getInt("level", 1);
        p.exp = sp.getInt("exp", 0);
        p.gold = sp.getInt("gold", 0);
        p.weaponId = sp.getInt("weaponId", 0);
        p.armorId = sp.getInt("armorId", 0);
        p.potions = sp.getInt("potions", 0);
        p.maxStage = sp.getInt("maxStage", 1);
        p.clearedMask = sp.getInt("clearedMask", 0);
        p.turnMode = sp.getInt("turnMode", MODE_FREE);
        p.turnSeconds = Math.max(MIN_TURN_SECONDS, Math.min(MAX_TURN_SECONDS,
                sp.getInt("turnSeconds", GameData.TURN_SECONDS)));

        p.gachaTickets = sp.getInt("gachaTickets", 0);
        p.gachaPity = sp.getInt("gachaPity", 0);


        String ow = sp.getString("ownedWeapons", null);
        String oa = sp.getString("ownedArmors", null);
        if (ow == null) for (int i = 0; i <= p.weaponId; i++) p.ownedWeapons.add(i); else parse(ow, p.ownedWeapons);
        if (oa == null) for (int i = 0; i <= p.armorId; i++) p.ownedArmors.add(i); else parse(oa, p.ownedArmors);
        p.ownedWeapons.add(0); p.ownedArmors.add(0);
        p.ownedWeapons.add(p.weaponId); p.ownedArmors.add(p.armorId);

        p.plankDay = sp.getString("plankDay", "");
        p.plankDailyMs = sp.getLong("plankDailyMs", 0);
        p.plankClaimedMask = sp.getInt("plankClaimedMask", 0);
        p.plankRemainderMs = sp.getLong("plankRemainderMs", 0);
        p.pushupToday = sp.getInt("pushupToday", 0);
        p.rollDaily();
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
                .putInt("maxStage", maxStage)
                .putInt("clearedMask", clearedMask)
                .putInt("turnMode", turnMode)
                .putInt("turnSeconds", turnSeconds)
                .putInt("gachaTickets", gachaTickets)
                .putInt("gachaPity", gachaPity)
                .putString("ownedWeapons", join(ownedWeapons))
                .putString("ownedArmors", join(ownedArmors))
                .putString("plankDay", plankDay)
                .putLong("plankDailyMs", plankDailyMs)
                .putInt("plankClaimedMask", plankClaimedMask)
                .putLong("plankRemainderMs", plankRemainderMs)
                .putInt("pushupToday", pushupToday)
                .apply();
    }

    private static void parse(String csv, Set<Integer> out) {
        for (String s : csv.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            try { out.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) { }
        }
    }

    private static String join(Set<Integer> set) {
        StringBuilder sb = new StringBuilder();
        for (int i : set) { if (sb.length() > 0) sb.append(','); sb.append(i); }
        return sb.toString();
    }

    public boolean ownsWeapon(int i) { return ownedWeapons.contains(i); }
    public boolean ownsArmor(int i) { return ownedArmors.contains(i); }

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

    public boolean isCleared(int stage) {
        return (clearedMask >> stage & 1) != 0;
    }

    /** Thắng ván `stage`: đánh dấu và mở khoá ván kế tiếp. */
    public void markCleared(int stage) {
        clearedMask |= 1 << stage;
        maxStage = Math.min(GameData.TOTAL_STAGES, Math.max(maxStage, stage + 1));
    }

    /** Mảng chỉ số 1..TOTAL_STAGES cho StageMapView. */
    public boolean[] clearedArray() {
        boolean[] a = new boolean[GameData.TOTAL_STAGES + 1];
        for (int i = 1; i <= GameData.TOTAL_STAGES; i++) a[i] = isCleared(i);
        return a;
    }

    public int clearedCount() {
        int n = 0;
        for (int i = 1; i <= GameData.TOTAL_STAGES; i++) if (isCleared(i)) n++;
        return n;
    }

    // ------------------------------------------------------------------ Plank

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public void rollDaily() {
        String t = today();
        if (!t.equals(plankDay)) {
            plankDay = t;
            plankDailyMs = 0;
            plankClaimedMask = 0;
            pushupToday = 0;
        }
    }

    /** Ghi nhận 1 rep hít đất thật vừa hoàn thành. */
    public void addPushUp() {
        rollDaily();
        pushupToday++;
    }

    /**
     * Cộng `ms` mili-giây Plank chuẩn. Trả về {EXP vừa nhận, vé vừa nhận, số cấp vừa lên}.
     * Cứ đủ 3 giây = 1 EXP; vé gacha nhận khi tổng thời gian trong ngày chạm các mốc 60s/150s/240s/300s.
     */
    public int[] addPlankTime(long ms) {
        rollDaily();
        plankDailyMs += ms;
        plankRemainderMs += ms;
        int gainedExp = (int) (plankRemainderMs / PLANK_MS_PER_EXP);
        plankRemainderMs %= PLANK_MS_PER_EXP;
        int levels = gainedExp > 0 ? addExp(gainedExp) : 0;

        int tickets = 0;
        for (int i = 0; i < PLANK_MILESTONE_S.length; i++) {
            if ((plankClaimedMask >> i & 1) == 0 && plankDailyMs >= PLANK_MILESTONE_S[i] * 1000) {
                plankClaimedMask |= 1 << i;
                tickets += PLANK_MILESTONE_TICKETS[i];
            }
        }
        gachaTickets += tickets;
        return new int[]{gainedExp, tickets, levels};
    }
}
