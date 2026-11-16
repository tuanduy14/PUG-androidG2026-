package com.example.pugcore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/** Rương báu pixel art (lưới 16x16): đóng / mở phát sáng theo màu phẩm chất. */
public class ChestView extends View {
    private static final List<PixelArt.R> CLOSED = PixelArt.parse(
        "1,14,14,2,#000000,0.35;2,7,12,7,#92400e;2,11,12,3,#78350f;2,3,12,5,#b45309;3,2,10,1,#b45309;3,3,10,1,#d97706;"
      + "2,7,12,1,#fbbf24;4,3,1,11,#fbbf24;11,3,1,11,#fbbf24;7,7,2,4,#fde047;7,8,2,1,#451a03;2,13,1,1,#fde047;13,13,1,1,#fde047");
    private static final List<PixelArt.R> OPEN = PixelArt.parse(
        "1,14,14,2,#000000,0.35;2,8,12,6,#92400e;2,11,12,3,#78350f;3,8,10,2,#451a03;"
      + "4,4,8,4,*#fde047,0.30;5,5,6,3,*#fde047,0.35;6,6,4,2,*#fde047,0.55;4,8,8,1,*#fde047;"
      + "2,8,12,1,#fbbf24;4,8,1,6,#fbbf24;11,8,1,6,#fbbf24;7,9,2,3,#fde047;7,10,2,1,#451a03;"
      + "3,1,10,3,#b45309;3,1,10,1,#d97706;4,1,1,3,#fbbf24;11,1,1,3,#fbbf24");

    private final Paint paint = new Paint();
    private boolean open = false;
    private int glow = 0xFFFDE047;

    public ChestView(Context c) { this(c, null); }

    public ChestView(Context c, AttributeSet a) {
        super(c, a);
        paint.setAntiAlias(false);
    }

    public void setOpen(boolean open, int glowColor) {
        this.open = open;
        this.glow = glowColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight(), size = Math.min(w, h);
        c.save();
        c.translate((w - size) / 2f, (h - size) / 2f);
        c.scale(size / 16f, size / 16f);
        PixelArt.draw(c, paint, open ? OPEN : CLOSED, 0, 0, true, glow);
        c.restore();
    }
}
