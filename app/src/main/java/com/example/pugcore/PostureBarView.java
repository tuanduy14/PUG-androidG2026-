package com.example.pugcore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Thước đo góc lưng (vai–hông–cổ chân): vùng chuẩn 165°–180° màu xanh, kim trắng là góc hiện tại. */
public class PostureBarView extends View {
    public static final double MIN_DEG = 140, MAX_DEG = 180, OK_MIN = 165;

    private final Paint p = new Paint();
    private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d;
    private double angle = Double.NaN;
    private boolean ok = false;

    public PostureBarView(Context c) { this(c, null); }

    public PostureBarView(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        p.setAntiAlias(false);
        t.setTextAlign(Paint.Align.CENTER);
    }

    public void setAngle(double deg, boolean ok) {
        this.angle = deg;
        this.ok = ok;
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), (int) (44 * d));
    }

    private void rect(Canvas c, float l, float tp, float r, float b, int color) {
        p.setColor(color);
        c.drawRect(l, tp, r, b, p);
    }

    private float xFor(double deg, float l, float r) {
        double f = (Math.max(MIN_DEG, Math.min(MAX_DEG, deg)) - MIN_DEG) / (MAX_DEG - MIN_DEG);
        return (float) (l + (r - l) * f);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), pad = 14 * d, b = 2 * d;
        float l = pad, r = w - pad, top = 6 * d, bot = 22 * d;
        float okX = xFor(OK_MIN, l, r);

        rect(c, l, top, r, bot, 0xFF05070C);
        rect(c, l + b, top + b, okX, bot - b, 0xFF7F1D1D);   // võng lưng / chổng mông
        rect(c, okX, top + b, r - b, bot - b, ok ? 0xFF10B981 : 0xFF047857); // vùng chuẩn
        rect(c, l + b, top + b, r - b, top + b + 2 * d, 0x33FFFFFF);

        if (!Double.isNaN(angle)) {
            float x = xFor(angle, l, r), kw = 4 * d;
            rect(c, x - kw / 2 - d, top - 4 * d, x + kw / 2 + d, bot + 4 * d, PixelUi.OUTLINE);
            rect(c, x - kw / 2, top - 3 * d, x + kw / 2, bot + 3 * d, ok ? 0xFFFFFFFF : 0xFFFBBF24);
        }

        t.setTypeface(PixelUi.font);
        t.setTextSize(10 * d);
        t.setColor(0xFF94A3B8);
        c.drawText("140°", l + 12 * d, bot + 15 * d, t);
        t.setColor(0xFF6EE7B7);
        c.drawText("165°", okX, bot + 15 * d, t);
        c.drawText("180°", r - 12 * d, bot + 15 * d, t);
    }
}
