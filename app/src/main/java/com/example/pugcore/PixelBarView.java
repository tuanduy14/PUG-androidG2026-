package com.example.pugcore;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Thanh HP / EXP / thời gian kiểu pixel (track tối + viền + vạch sáng phía trên). */
public class PixelBarView extends View {
    private final Paint p = new Paint();
    private float frac = 0f, shown = 0f;
    private int fill = 0xFFF43F5E;
    private ValueAnimator anim;

    public PixelBarView(Context c) { this(c, null); }
    public PixelBarView(Context c, AttributeSet a) { super(c, a); p.setAntiAlias(false); }

    public void setFillColor(int color) { fill = color; invalidate(); }

    public void setFraction(float f, boolean animate) {
        f = Math.max(0f, Math.min(1f, f));
        if (anim != null) anim.cancel();
        frac = f;
        if (!animate || Math.abs(f - shown) < 0.001f) { shown = f; invalidate(); return; }
        anim = ValueAnimator.ofFloat(shown, f);
        anim.setDuration(150);
        anim.addUpdateListener(a -> { shown = (float) a.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density, b = 2 * d;
        int w = getWidth(), h = getHeight();
        p.setColor(0xFF05070C); c.drawRect(0, 0, w, h, p);
        p.setColor(0xFF0D101A); c.drawRect(b, b, w - b, h - b, p);
        float fw = (w - 2 * b) * shown;
        if (fw > 0) {
            p.setColor(fill); c.drawRect(b, b, b + fw, h - b, p);
            p.setColor(0x4DFFFFFF); c.drawRect(b, b, b + fw, b + 2 * d, p);
        }
    }
}
