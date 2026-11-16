package com.example.pugcore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/** Nền pixel art (đạo trường, đấu trường, màn hình chính). Tỉ lệ kiểu "slice" giống SVG gốc. */
public class SceneBackgroundView extends View {
    public static final int DOJO = 0, BATTLE = 1, TITLE = 2;

    private static final String DOJO_SPEC =
        "0,0,180,160,#21283d;0,14,180,4,#171d2e;74,18,32,48,#7f1d1d;76,20,28,44,#991b1b;82,28,16,2,#fde047;" +
        "89,26,2,24,#fde047;84,38,12,2,#fde047;10,0,10,160,#451a03;160,0,10,160,#451a03;12,0,2,160,#78350f;" +
        "162,0,2,160,#78350f;36,18,8,12,#f59e0b;38,20,4,8,#fef08a;136,18,8,12,#f59e0b;138,20,4,8,#fef08a;" +
        "0,160,180,80,#573b26;0,160,180,4,#7c5335;0,182,180,2,#422a19;0,206,180,2,#422a19";
    private static final String BATTLE_TAIL =
        "0,20,180,2,#10121e;0,50,180,2,#10121e;0,80,180,2,#10121e;44,22,2,28,#10121e;128,22,2,28,#10121e;" +
        "86,52,2,28,#10121e;20,34,4,14,#78350f;19,26,6,8,#f97316;21,28,2,4,#fef08a;156,34,4,14,#78350f;" +
        "155,26,6,8,#f97316;157,28,2,4,#fef08a;0,125,180,115,#23283b;0,125,180,4,#323954";
    private static final String TITLE_SPEC =
        "0,0,180,80,#0d1326;0,80,180,70,#16203a;0,150,180,60,#1e2b4d;132,26,22,22,#fef08a;136,30,5,5,#fde047;" +
        "144,38,4,4,#fde047;22,28,2,2,#e2e8f0;54,42,2,2,#93c5fd;92,20,2,2,#fde047;74,64,2,2,#e2e8f0;" +
        "112,54,2,2,#e2e8f0;36,96,2,2,#93c5fd;14,165,28,55,#0f172a;12,158,8,8,#0f172a;24,158,8,8,#0f172a;" +
        "36,158,8,8,#0f172a;136,172,32,48,#0f172a;146,156,12,18,#0f172a;150,162,4,6,#f59e0b;0,218,180,12,#15803d;" +
        "0,230,180,90,#1c1917;0,232,180,4,#292524";

    private final Paint paint = new Paint();
    private List<PixelArt.R> rects;
    private float vw = 180, vh = 240;

    public SceneBackgroundView(Context c) { this(c, null); }
    public SceneBackgroundView(Context c, AttributeSet a) { super(c, a); paint.setAntiAlias(false); }

    public void setScene(int scene, boolean boss) {
        switch (scene) {
            case BATTLE:
                rects = PixelArt.parse("0,0,180,125," + (boss ? "#22131d" : "#181b2c") + ";" + BATTLE_TAIL);
                vw = 180; vh = 240; break;
            case TITLE:
                rects = PixelArt.parse(TITLE_SPEC); vw = 180; vh = 320; break;
            default:
                rects = PixelArt.parse(DOJO_SPEC); vw = 180; vh = 240;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (rects == null) return;
        float s = Math.max(getWidth() / vw, getHeight() / vh);
        c.save();
        c.translate((getWidth() - vw * s) / 2f, (getHeight() - vh * s) / 2f);
        c.scale(s, s);
        PixelArt.draw(c, paint, rects);
        c.restore();
    }
}
