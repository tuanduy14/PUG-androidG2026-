package com.example.pugcore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Thanh trượt kiểu pixel: rãnh tối có vạch chia, phần đã chọn tô màu, núm vuông có viền. */
public class PixelSliderView extends View {

    public interface Listener {
        void onChanged(int value, boolean fromUser);
        default void onStop(int value) { }
    }

    private final Paint p = new Paint();
    private final float d;
    private int min = 0, max = 100, value = 50;
    private int fill = 0xFF38BDF8;
    private Listener listener;

    public PixelSliderView(Context c) { this(c, null); }

    public PixelSliderView(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        p.setAntiAlias(false);
    }

    public void setRange(int min, int max) { this.min = min; this.max = max; invalidate(); }
    public void setValue(int v) { value = Math.max(min, Math.min(max, v)); invalidate(); }
    public int getValue() { return value; }
    public void setFillColor(int c) { fill = c; invalidate(); }
    public void setListener(Listener l) { listener = l; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), (int) (32 * d));
    }

    private void rect(Canvas c, float l, float t, float r, float b, int color) {
        p.setColor(color);
        c.drawRect(l, t, r, b, p);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float knobW = 14 * d, knobH = 26 * d, trackH = 12 * d, b = 2 * d;
        float cy = h / 2f, top = cy - trackH / 2f, bottom = cy + trackH / 2f;
        float frac = max == min ? 0f : (value - min) / (float) (max - min);
        float kx = knobW / 2f + (w - knobW) * frac;

        rect(c, 0, top, w, bottom, 0xFF05070C);              // viền rãnh
        rect(c, b, top + b, w - b, bottom - b, 0xFF0D101A);  // lòng rãnh
        if (kx > b) {
            rect(c, b, top + b, kx, bottom - b, fill);       // phần đã chọn
            rect(c, b, top + b, kx, top + b + 2 * d, 0x4DFFFFFF);
        }
        for (int i = 1; i < 10; i++) {                       // vạch chia 10%
            float x = w * i / 10f;
            rect(c, x - d / 2f, top + b, x + d / 2f, bottom - b, 0x59000000);
        }
        // núm
        rect(c, kx - knobW / 2f, cy - knobH / 2f, kx + knobW / 2f, cy + knobH / 2f, PixelUi.OUTLINE);
        rect(c, kx - knobW / 2f + b, cy - knobH / 2f + b, kx + knobW / 2f - b, cy + knobH / 2f - b, 0xFFF1F5F9);
        rect(c, kx - knobW / 2f + b, cy + knobH / 2f - b - 4 * d, kx + knobW / 2f - b, cy + knobH / 2f - b, 0xFF94A3B8);
    }

    private void update(float x) {
        float knobW = 14 * d, w = getWidth();
        float f = Math.max(0f, Math.min(1f, (x - knobW / 2f) / Math.max(1f, w - knobW)));
        int v = min + Math.round(f * (max - min));
        if (v != value) {
            value = v;
            invalidate();
            if (listener != null) listener.onChanged(v, true);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                update(e.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                update(e.getX());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                if (listener != null) listener.onStop(value);
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }
}
