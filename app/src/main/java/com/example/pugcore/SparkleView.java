package com.example.pugcore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

/** Các tia sáng pixel nhấp nháy theo từng nấc (crispEdges) — dùng cho trang bị Sử thi/Huyền thoại. */
public class SparkleView extends View {
    private final Paint p = new Paint();
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Random rnd = new Random();
    private float[] px = new float[0], py = new float[0];
    private int[] phase = new int[0];
    private int color = 0xFFFFFFFF;
    private int frame = 0;
    private boolean running = false;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            frame++;
            invalidate();
            h.postDelayed(this, 140);
        }
    };

    public SparkleView(Context c) { this(c, null); }

    public SparkleView(Context c, AttributeSet a) {
        super(c, a);
        p.setAntiAlias(false);
    }

    /** Đặt màu và số tia rồi bắt đầu nhấp nháy. */
    public void start(int color, int count) {
        this.color = color;
        px = new float[count];
        py = new float[count];
        phase = new int[count];
        for (int i = 0; i < count; i++) {
            px[i] = rnd.nextFloat();
            py[i] = rnd.nextFloat();
            phase[i] = rnd.nextInt(4);
        }
        running = true;
        h.removeCallbacks(tick);
        h.post(tick);
        setVisibility(VISIBLE);
    }

    public void stop() {
        running = false;
        h.removeCallbacks(tick);
        setVisibility(INVISIBLE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        running = false;
        h.removeCallbacks(tick);
    }

    @Override
    protected void onDraw(Canvas c) {
        float u = 3 * getResources().getDisplayMetrics().density;
        int w = getWidth(), hgt = getHeight();
        for (int i = 0; i < px.length; i++) {
            int s = (frame + phase[i]) % 4;
            if (s == 0) continue;
            float x = Math.round(px[i] * (w - 3 * u) / u) * u + u;
            float y = Math.round(py[i] * (hgt - 3 * u) / u) * u + u;
            p.setColor(i % 3 == 0 ? 0xFFFFFFFF : color);
            c.drawRect(x, y, x + u, y + u, p);
            if (s == 2) { // dạng dấu cộng
                c.drawRect(x - u, y, x, y + u, p);
                c.drawRect(x + u, y, x + 2 * u, y + u, p);
                c.drawRect(x, y - u, x + u, y, p);
                c.drawRect(x, y + u, x + u, y + 2 * u, p);
            }
        }
    }
}
