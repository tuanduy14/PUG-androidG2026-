package com.example.pugcore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Vẽ các điểm và đường nối của cơ thể lên trên hình camera (khớp với FILL_CENTER). */
public class PoseOverlayView extends View {

    // Nếu khung xương bị ngược trái-phải so với hình, đổi false <-> true
    private static final boolean INVERT_MIRROR = false;
    private static final float MIN_DRAW_VISIBILITY = 0.3f;

    private static final int[][] CONNECTIONS = {
            {11, 12}, {11, 13}, {13, 15}, {12, 14}, {14, 16},
            {11, 23}, {12, 24}, {23, 24},
            {23, 25}, {25, 27}, {24, 26}, {26, 28},
            {15, 17}, {15, 19}, {15, 21}, {16, 18}, {16, 20}, {16, 22},
            {27, 29}, {27, 31}, {28, 30}, {28, 32}
    };

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint armPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private PoseFrame frame;

    public PoseOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint.setColor(0xFF4CAF50);
        linePaint.setStrokeWidth(6f);
        armPaint.setColor(0xFFFFEB3B);
        armPaint.setStrokeWidth(12f);
        pointPaint.setColor(0xFFFFFFFF);
    }

    public void setFrame(PoseFrame frame) {
        this.frame = frame;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        PoseFrame f = frame;
        if (f == null || !f.hasPerson() || f.width <= 0 || f.height <= 0) return;

        float vw = getWidth(), vh = getHeight();
        float scale = Math.max(vw / f.width, vh / f.height); // FILL_CENTER
        float dx = (f.width * scale - vw) / 2f;
        float dy = (f.height * scale - vh) / 2f;
        boolean mirror = f.mirrored ^ INVERT_MIRROR;

        for (int[] c : CONNECTIONS) {
            draw(canvas, f, c[0], c[1], linePaint, scale, dx, dy, mirror);
        }

        // Tô nổi bật cánh tay đang dùng để đếm
        boolean left = "trái".equals(f.arm);
        draw(canvas, f, left ? 11 : 12, left ? 13 : 14, armPaint, scale, dx, dy, mirror);
        draw(canvas, f, left ? 13 : 14, left ? 15 : 16, armPaint, scale, dx, dy, mirror);

        for (int i = 11; i < f.x.length && i <= 32; i++) {
            if (f.visibility[i] < MIN_DRAW_VISIBILITY) continue;
            canvas.drawCircle(px(f, i, scale, dx, mirror, vw), py(f, i, scale, dy), 8f, pointPaint);
        }
    }

    private void draw(Canvas canvas, PoseFrame f, int a, int b, Paint paint,
                      float scale, float dx, float dy, boolean mirror) {
        if (a >= f.x.length || b >= f.x.length) return;
        if (f.visibility[a] < MIN_DRAW_VISIBILITY || f.visibility[b] < MIN_DRAW_VISIBILITY) return;
        float vw = getWidth();
        canvas.drawLine(px(f, a, scale, dx, mirror, vw), py(f, a, scale, dy),
                px(f, b, scale, dx, mirror, vw), py(f, b, scale, dy), paint);
    }

    private static float px(PoseFrame f, int i, float scale, float dx, boolean mirror, float vw) {
        float p = f.x[i] * f.width * scale - dx;
        return mirror ? vw - p : p;
    }

    private static float py(PoseFrame f, int i, float scale, float dy) {
        return f.y[i] * f.height * scale - dy;
    }
}
