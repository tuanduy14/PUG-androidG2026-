package com.example.pugcore;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Tiện ích giao diện pixel: font, nút 3D, khung viền. */
public final class PixelUi {
    private PixelUi() {}

    public static final int OUTLINE = 0xFF090C14;
    public static final int AMBER = 0xFFF59E0B, AMBER_LIGHT = 0xFFFCD34D, SKY = 0xFF7DD3FC, EMERALD = 0xFF6EE7B7,
            ROSE = 0xFFFDA4AF, SLATE = 0xFF94A3B8, TEXT = 0xFFF1F5F9, DARK_TEXT = 0xFF020617;

    public static Typeface font = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);

    /** Đặt file Pixelify Sans vào app/src/main/assets/fonts/pixelify.ttf; thiếu file thì dùng monospace. */
    public static void init(Context c) {
        try {
            Typeface base = Typeface.createFromAsset(c.getAssets(), "fonts/pixelify.ttf");
            font = Typeface.create(base, Typeface.BOLD);
        } catch (Exception ignored) { }
    }

    public static float dp(Context c, float v) { return v * c.getResources().getDisplayMetrics().density; }

    public static void applyFont(View v) {
        if (v instanceof TextView) ((TextView) v).setTypeface(font);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) applyFont(g.getChildAt(i));
        }
    }

    public static int shade(int c, float f) {
        return Color.argb(255, (int) (Color.red(c) * f), (int) (Color.green(c) * f), (int) (Color.blue(c) * f));
    }

    /** Khung phẳng có viền dày. */
    public static Drawable box(Context c, int fill, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setStroke((int) dp(c, strokeDp), stroke);
        return g;
    }

    private static Drawable face(Context c, int fill, int top, int bottom) {
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(OUTLINE);
        GradientDrawable f = new GradientDrawable();
        f.setColor(fill);
        f.setStroke((int) dp(c, 3), OUTLINE);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{shadow, f});
        ld.setLayerInset(1, 0, top, 0, bottom);
        return ld;
    }

    /**
     * Nút 3D kiểu pixel: mặt nút cao hơn bóng 4dp, nhấn xuống thì lún.
     * Lưu ý: view dùng nền này nên để paddingBottom lớn hơn paddingTop ~4dp.
     */
    public static Drawable button(Context c, int fill) {
        int s4 = (int) dp(c, 4), s3 = (int) dp(c, 3), s1 = (int) dp(c, 1);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, face(c, shade(fill, 0.85f), s3, s1));
        s.addState(new int[]{}, face(c, fill, 0, s4));
        return s;
    }

    public static void setEnabledLook(View v, boolean enabled) {
        v.setEnabled(enabled);
        v.setClickable(enabled);
        v.setAlpha(enabled ? 1f : 0.52f);
    }
}
