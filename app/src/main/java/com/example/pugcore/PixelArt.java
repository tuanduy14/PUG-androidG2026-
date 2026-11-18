package com.example.pugcore;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class PixelArt {
    private PixelArt() {}

    public static final class R {
        final int x, y, w, h, color, alpha; final boolean tint;
        R(int x, int y, int w, int h, int color, int alpha, boolean tint) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.color = color; this.alpha = alpha; this.tint = tint;
        }
    }

    public static List<R> parse(String spec) {
        List<R> out = new ArrayList<>();
        for (String part : spec.split(";")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            String[] t = part.split(",");
            boolean tint = t[4].startsWith("*");
            int color = Color.parseColor(tint ? t[4].substring(1) : t[4]);
            int alpha = t.length > 5 ? Math.round(Float.parseFloat(t[5]) * 255) : 255;
            out.add(new R(Integer.parseInt(t[0].trim()), Integer.parseInt(t[1].trim()),
                    Integer.parseInt(t[2].trim()), Integer.parseInt(t[3].trim()), color, alpha, tint));
        }
        return out;
    }

    public static void draw(Canvas c, Paint p, List<R> rects, float ox, float oy, boolean hasTint, int tint) {
        for (R r : rects) {
            p.setColor((r.tint && hasTint) ? tint : r.color);
            p.setAlpha(r.alpha);
            c.drawRect(ox + r.x, oy + r.y, ox + r.x + r.w, oy + r.y + r.h, p);
        }
    }

    public static void draw(Canvas c, Paint p, List<R> rects) { draw(c, p, rects, 0, 0, false, 0); }

    /** Vẽ icon 12x12 vào ô vuông (left, top, size px). */
    public static void drawIcon(Canvas c, Paint p, String name, float left, float top, float size) {
        drawIcon(c, p, name, left, top, size, false, 0);
    }

    /** Như trên nhưng đổi màu các phần tử có thể tint (icon có dấu '*'). */
    public static void drawIcon(Canvas c, Paint p, String name, float left, float top, float size,
                                boolean hasTint, int tint) {
        List<R> r = ICONS.get(name);
        if (r == null) return;
        c.save();
        c.translate(left, top);
        c.scale(size / 12f, size / 12f);
        draw(c, p, r, 0, 0, hasTint, tint);
        c.restore();
    }

    // ================= Quái vật (lưới 20x20) =================
    private static final Map<GameData.Archetype, List<R>> MONSTERS = new HashMap<>();
    static {
        MONSTERS.put(GameData.Archetype.SLIME, parse(
            "3,18,14,2,#000000,0.45;5,7,10,2,#22c55e;3,9,14,6,#16a34a;2,13,16,5,#15803d;5,9,3,2,#86efac;6,8,2,1,#bbf7d0;" +
            "6,12,2,2,#052e16;12,12,2,2,#052e16;6,12,1,1,#ffffff;12,12,1,1,#ffffff;8,15,4,1,#052e16"));
        MONSTERS.put(GameData.Archetype.GOBLIN, parse(
            "3,18,14,2,#000000,0.45;2,5,3,2,#65a30d;15,5,3,2,#65a30d;5,4,10,6,#84cc16;6,6,2,2,#dc2626;12,6,2,2,#dc2626;" +
            "7,9,6,1,#fef08a;6,10,8,5,#78350f;5,11,2,4,#84cc16;13,11,2,4,#84cc16;2,9,2,4,#cbd5e1;2,13,2,2,#451a03;" +
            "6,15,3,3,#4d7c0f;11,15,3,3,#4d7c0f"));
        MONSTERS.put(GameData.Archetype.SKELETON, parse(
            "3,18,14,2,#000000,0.45;6,2,8,6,#f1f5f9;7,8,6,2,#cbd5e1;7,4,2,2,#0f172a;11,4,2,2,#0f172a;7,4,1,1,#ef4444;" +
            "11,4,1,1,#ef4444;9,10,2,5,#cbd5e1;6,11,8,1,#f1f5f9;6,13,8,1,#f1f5f9;15,5,2,7,#94a3b8;14,12,4,1,#475569;" +
            "7,15,2,3,#e2e8f0;11,15,2,3,#e2e8f0"));
        MONSTERS.put(GameData.Archetype.ORC, parse(
            "3,18,14,2,#000000,0.45;5,2,10,7,#15803d;6,4,2,2,#facc15;12,4,2,2,#facc15;6,7,2,2,#fefce8;12,7,2,2,#fefce8;" +
            "3,9,14,6,#475569;5,10,10,4,#334155;1,6,3,5,#94a3b8;2,11,1,4,#78350f;5,15,4,3,#1e293b;11,15,4,3,#1e293b"));
        MONSTERS.put(GameData.Archetype.DRAGON, parse(
            "3,18,14,2,#000000,0.45;1,4,4,7,#991b1b;15,4,4,7,#991b1b;2,3,2,2,#f59e0b;16,3,2,2,#f59e0b;5,1,2,3,#fde047;" +
            "13,1,2,3,#fde047;6,3,8,6,#dc2626;7,5,2,2,#fef08a;11,5,2,2,#fef08a;5,9,10,7,#b91c1c;7,10,6,5,#fbbf24;" +
            "5,16,3,2,#7f1d1d;12,16,3,2,#7f1d1d"));
    }

    public static List<R> monster(GameData.Archetype a) { return MONSTERS.get(a); }

    // ================= Icon (lưới 12x12) =================
    public static final Map<String, List<R>> ICONS = new HashMap<>();
    static {
        ICONS.put("coin", parse("3,1,6,10,#eab308;1,3,10,6,#eab308;2,2,8,8,#facc15;5,3,2,6,#ca8a04;3,3,2,2,#fef08a"));
        ICONS.put("heart", parse("2,2,3,3,#ef4444;7,2,3,3,#ef4444;1,3,10,4,#ef4444;2,7,8,2,#dc2626;4,9,4,2,#dc2626;5,11,2,1,#dc2626;2,3,2,1,#fca5a5"));
        ICONS.put("exp", parse("5,1,2,10,#38bdf8;1,5,10,2,#38bdf8;3,3,6,6,#0ea5e9;5,4,2,4,#e0f2fe"));
        ICONS.put("sword", parse("8,1,3,3,*#e2e8f0;6,3,3,3,*#cbd5e1;4,5,3,3,*#cbd5e1;2,6,4,2,#eab308;1,9,2,2,#78350f;3,8,2,2,#a16207"));
        ICONS.put("shield", parse("2,1,8,6,*#94a3b8;3,7,6,2,*#64748b;5,9,2,2,*#475569;4,3,4,3,#e2e8f0"));
        ICONS.put("potion", parse("4,1,4,2,#d97706;5,3,2,2,#bae6fd;2,5,8,5,#bae6fd;3,6,6,4,*#ef4444;3,10,6,1,#991b1b;4,6,1,2,#fecaca"));
        ICONS.put("dumbbell", parse("1,3,2,6,#94a3b8;9,3,2,6,#94a3b8;3,5,6,2,#e2e8f0;0,4,1,4,#facc15;11,4,1,4,#facc15"));
        ICONS.put("swords", parse("1,1,2,2,#e2e8f0;3,3,2,2,#e2e8f0;5,5,2,2,#cbd5e1;9,1,2,2,#e2e8f0;7,3,2,2,#e2e8f0;3,7,2,2,#eab308;7,7,2,2,#eab308;1,9,2,2,#78350f;9,9,2,2,#78350f"));
        ICONS.put("shop", parse("1,2,10,3,#ef4444;3,2,2,3,#fef2f2;7,2,2,3,#fef2f2;2,5,8,6,#d97706;4,7,4,4,#451a03"));
        ICONS.put("crown", parse("1,3,2,6,#facc15;5,2,2,7,#facc15;9,3,2,6,#facc15;3,5,2,4,#eab308;7,5,2,4,#eab308;1,9,10,2,#ca8a04;5,6,2,2,#ef4444"));
        ICONS.put("lock", parse("4,2,4,1,#94a3b8;3,3,1,2,#94a3b8;8,3,1,2,#94a3b8;2,5,8,6,#64748b;5,7,2,2,#0f172a"));
        ICONS.put("check", parse("9,2,2,2,*#4ade80;7,4,2,2,*#4ade80;5,6,2,2,*#4ade80;3,8,2,2,*#4ade80;1,6,2,2,*#4ade80"));
        ICONS.put("map", parse("1,2,3,8,#fde68a;4,3,4,8,#fcd34d;8,2,3,8,#fde68a;2,4,2,2,#ef4444;5,6,2,2,#3b82f6;8,4,2,2,#16a34a"));
        ICONS.put("ticket", parse("1,3,10,6,#fbbf24;1,3,10,1,#fde68a;5,3,1,6,#b45309;3,5,1,2,#b45309;8,5,1,2,#b45309;0,5,1,2,#78350f;11,5,1,2,#78350f"));
        ICONS.put("gear", parse("5,0,2,2,#94a3b8;5,10,2,2,#94a3b8;0,5,2,2,#94a3b8;10,5,2,2,#94a3b8;1,1,2,2,#94a3b8;9,1,2,2,#94a3b8;1,9,2,2,#94a3b8;9,9,2,2,#94a3b8;2,2,8,8,#cbd5e1;3,3,6,6,#94a3b8;4,4,4,4,#cbd5e1;5,5,2,2,#1e293b"));
    }

    // ================= Hiệp sĩ (lưới 20x20) =================
    public static final int POSE_IDLE = 0, POSE_TRAIN_DOWN = 1, POSE_TRAIN_UP = 2, POSE_ATTACK = 3, POSE_HURT = 4;

    private static void rc(Canvas c, Paint p, int x, int y, int w, int h, int color, int alpha) {
        p.setColor(color);
        p.setAlpha(alpha);
        c.drawRect(x, y, x + w, y + h, p);
    }

    private static void rc(Canvas c, Paint p, int x, int y, int w, int h, String hex) {
        rc(c, p, x, y, w, h, Color.parseColor(hex), 255);
    }

    public static void drawHero(Canvas c, Paint p, int pose, int pP, int pA, int blade, int hilt, boolean hasWeapon) {
        boolean down = pose == POSE_TRAIN_DOWN, up = pose == POSE_TRAIN_UP, atk = pose == POSE_ATTACK;
        int o = down ? 2 : 0;
        final int skin = Color.parseColor("#fde68a");

        rc(c, p, 4, 18, 12, 2, 0xFF000000, 115);
        // chân & giày
        rc(c, p, 6, 15, 3, 3, "#334155"); rc(c, p, 11, 15, 3, 3, "#334155");
        rc(c, p, 5, 17, 4, 2, "#451a03"); rc(c, p, 11, 17, 4, 2, "#451a03");
        // thân & giáp
        rc(c, p, 5, 9 + o, 10, 6, pP, 255);
        rc(c, p, 6, 10 + o, 8, 4, pA, 255);
        rc(c, p, 5, 14 + o, 10, 1, "#78350f"); rc(c, p, 9, 14 + o, 2, 1, "#facc15");
        // đầu
        rc(c, p, 6, 3 + o, 8, 6, skin, 255);
        rc(c, p, 5, 2 + o, 10, 2, pP, 255);
        rc(c, p, 7, 1 + o, 6, 1, "#ef4444");
        rc(c, p, 6, 4 + o, 8, 1, "#ef4444");
        rc(c, p, 7, 6 + o, 1, 2, "#0f172a"); rc(c, p, 12, 6 + o, 1, 2, "#0f172a");
        rc(c, p, 6, 7 + o, 1, 1, "#fca5a5"); rc(c, p, 13, 7 + o, 1, 1, "#fca5a5");

        if (down || up) {
            int ay = up ? 6 : 9, by = up ? 5 : 8, wy = up ? 3 : 6, gy = up ? 4 : 7;
            rc(c, p, 3, ay, 2, 3, skin, 255); rc(c, p, 15, ay, 2, 3, skin, 255);
            rc(c, p, 1, by, 18, 1, "#94a3b8");
            rc(c, p, 1, wy, 2, 5, "#475569"); rc(c, p, 17, wy, 2, 5, "#475569");
            rc(c, p, 0, gy, 1, 3, "#eab308"); rc(c, p, 19, gy, 1, 3, "#eab308");
        } else {
            rc(c, p, 3, 10 + o, 2, 4, pP, 255);
            rc(c, p, 2, 11 + o, 2, 4, "#64748b");
            rc(c, p, 2, 12 + o, 2, 2, pA, 255);
            rc(c, p, 15, atk ? 8 : 10 + o, 2, 3, skin, 255);
            if (hasWeapon) {
                int dy = atk ? -4 : 0;
                rc(c, p, 16, 8 + dy, 2, 3, hilt, 255);
                rc(c, p, 15, 7 + dy, 4, 1, hilt, 255);
                rc(c, p, 16, 1 + dy, 2, 6, blade, 255);
                rc(c, p, 17, 2 + dy, 1, 5, 0xFFFFFFFF, 115);
            }
        }
    }
}
