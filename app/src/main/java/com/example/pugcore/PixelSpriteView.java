package com.example.pugcore;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Vẽ hiệp sĩ / quái / icon pixel art. XML: app:pixelIcon="coin" cho icon. */
public class PixelSpriteView extends View {
    private static final int KIND_ICON = 0, KIND_HERO = 1, KIND_MONSTER = 2;
    private static final ColorMatrixColorFilter HURT_FILTER;
    static {
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.2f);
        cm.postConcat(new ColorMatrix(new float[]{2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 1, 0}));
        HURT_FILTER = new ColorMatrixColorFilter(cm);
    }

    private final Paint paint = new Paint();
    private int kind = KIND_ICON;
    private String icon = "coin";
    private boolean hasTint = false;
    private int tint = 0;
    private int pose = PixelArt.POSE_IDLE, armorP = 0xFF3B82F6, armorA = 0xFF1D4ED8, blade = 0xFFCBD5E1, hilt = 0xFFB45309;
    private GameData.Archetype archetype = GameData.Archetype.SLIME;
    private boolean hurt = false;

    public PixelSpriteView(Context c) { this(c, null); }

    public PixelSpriteView(Context c, AttributeSet attrs) {
        super(c, attrs);
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        if (attrs != null) {
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.PixelSpriteView);
            String s = a.getString(R.styleable.PixelSpriteView_pixelIcon);
            if (s != null) icon = s;
            a.recycle();
        }
    }

    public void setIcon(String name, Integer tintColor) {
        kind = KIND_ICON; icon = name;
        hasTint = tintColor != null; tint = tintColor == null ? 0 : tintColor;
        invalidate();
    }

    public void setHero(int pose, GameData.Armor armor, GameData.Weapon weapon) {
        kind = KIND_HERO; this.pose = pose;
        armorP = armor.primary; armorA = armor.accent; blade = weapon.blade; hilt = weapon.hilt;
        invalidate();
    }

    public void setHeroPose(int pose) { kind = KIND_HERO; this.pose = pose; invalidate(); }

    public void setMonster(GameData.Archetype a) { kind = KIND_MONSTER; archetype = a; invalidate(); }

    public void setHurt(boolean h) { hurt = h; invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight(), size = Math.min(w, h);
        float grid = kind == KIND_ICON ? 12f : 20f;
        paint.setColorFilter((hurt || pose == PixelArt.POSE_HURT && kind == KIND_HERO) ? HURT_FILTER : null);
        c.save();
        c.translate((w - size) / 2f, (h - size) / 2f);
        c.scale(size / grid, size / grid);
        if (kind == KIND_ICON) {
            PixelArt.draw(c, paint, PixelArt.ICONS.get(icon) == null ? PixelArt.ICONS.get("coin") : PixelArt.ICONS.get(icon), 0, 0, hasTint, tint);
        } else if (kind == KIND_MONSTER) {
            PixelArt.draw(c, paint, PixelArt.monster(archetype));
        } else {
            PixelArt.drawHero(c, paint, pose, armorP, armorA, blade, hilt, true);
        }
        c.restore();
    }
}
