package com.example.pugcore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Bản đồ 10 ván dọc (port từ StageMapView.tsx). Hệ toạ độ thế giới 180x580, cao = 3 × rộng.
 * Đặt trong ScrollView. Chạm vào ô ván đã mở khoá để chọn.
 */
public class StageMapView extends View {

    public interface Listener { void onStageTapped(int stage); }

    private static final int[][] XY = {{0, 0}, {126, 534}, {65, 482}, {42, 428}, {112, 374}, {122, 306},
            {54, 252}, {46, 198}, {124, 146}, {102, 94}, {84, 44}};
    private static final int[][] TREES = {{14, 518}, {26, 492}, {148, 464}, {158, 432}, {12, 382}, {152, 368},
            {16, 296}, {156, 274}, {148, 210}, {14, 150}};

    private static final List<PixelArt.R> BASE = PixelArt.parse(
        "0,336,180,244,#1b3022;12,360,44,32,#213b29;118,420,50,44,#213b29;20,490,68,40,#213b29;96,510,70,50,#16281c;" +
        "0,132,180,204,#1c2233;16,160,52,36,#232b40;108,216,56,40,#232b40;24,274,60,36,#252938;0,332,180,6,#141926;" +
        "0,0,180,132,#23141c;12,20,48,38,#2d1924;116,44,50,46,#2d1924;14,72,18,3,#dc2626;28,75,12,2,#f97316;" +
        "136,32,22,3,#dc2626;144,35,10,2,#facc15;18,26,14,3,#b91c1c;" +
        "0,330,180,18,#102945;0,332,180,14,#1d4f82;10,336,18,2,#60a5fa;46,340,22,2,#60a5fa;138,335,26,2,#60a5fa");
    private static final List<PixelArt.R> BRIDGE = PixelArt.parse(
        "109,328,24,22,#451a03;111,330,20,18,#92400e;111,334,20,1,#451a03;111,339,20,1,#451a03;111,344,20,1,#451a03;" +
        "109,328,2,22,#d97706;131,328,2,22,#d97706");
    private static final List<PixelArt.R> INN = PixelArt.parse(
        "2,10,22,14,#d6c4a3;0,6,26,5,#b45309;4,2,18,4,#d97706;10,15,6,9,#5c3a21;4,14,4,4,#facc15");
    private static final List<PixelArt.R> BANNER = PixelArt.parse("0,4,3,24,#451a03;3,5,14,10,#dc2626;6,8,6,4,#fef08a");
    private static final List<PixelArt.R> GATE = PixelArt.parse(
        "0,8,40,18,#1e293b;-2,4,8,10,#334155;16,0,8,10,#334155;34,4,8,10,#334155;14,14,12,12,#090c14;18,5,4,4,#ef4444");
    private static final List<PixelArt.R> TREE = PixelArt.parse(
        "5,14,4,5,#451a03;1,10,12,4,#14532d;2,6,10,4,#166534;4,2,6,4,#15803d;6,0,2,2,#22c55e");
    private static final List<PixelArt.R> TOMB1 = PixelArt.parse("0,2,10,12,#475569;2,0,6,2,#64748b;4,4,2,6,#1e293b;2,6,6,2,#1e293b");
    private static final List<PixelArt.R> TOMB2 = PixelArt.parse("0,4,12,10,#334155;2,2,8,2,#475569");
    private static final List<PixelArt.R> SPECKLE_A = PixelArt.parse("0,0,3,2,#2d2622,0.45");
    private static final List<PixelArt.R> SPECKLE_B = PixelArt.parse("0,0,3,2,#ffffff,0.15");

    private static final List<int[]> ROAD = new ArrayList<>(); // {x,y,w,h}
    static {
        int w = 14, half = w / 2;
        ROAD.add(new int[]{126 - half, 534, w, 46});
        for (int i = 1; i < 10; i++) {
            int[] a = XY[i], b = XY[i + 1];
            int midY = Math.round((a[1] + b[1]) / 2f);
            ROAD.add(new int[]{a[0] - half, Math.min(a[1], midY) - half, w, Math.abs(a[1] - midY) + w});
            ROAD.add(new int[]{Math.min(a[0], b[0]) - half, midY - half, Math.abs(b[0] - a[0]) + w, w});
            ROAD.add(new int[]{b[0] - half, Math.min(midY, b[1]) - half, w, Math.abs(midY - b[1]) + w});
        }
    }

    private final Paint paint = new Paint();
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d;
    private int maxUnlocked = 1, selected = 1;
    private boolean[] cleared = new boolean[11];
    private Listener listener;
    private final GestureDetector detector;

    public StageMapView(Context c) { this(c, null); }

    public StageMapView(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        paint.setAntiAlias(false);
        text.setTextAlign(Paint.Align.CENTER);
        detector = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }
            @Override public boolean onSingleTapUp(MotionEvent e) { handleTap(e.getX(), e.getY()); return true; }
        });
    }

    public void setListener(Listener l) { listener = l; }

    public void setState(int maxUnlocked, boolean[] cleared, int selected) {
        this.maxUnlocked = maxUnlocked; this.cleared = cleared; this.selected = selected;
        invalidate();
    }

    public float nodeCenterY(int stage) { return XY[stage][1] * (getHeight() / 580f); }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        setMeasuredDimension(w, (int) (w * 3f));
    }

    @Override public boolean onTouchEvent(MotionEvent e) { return detector.onTouchEvent(e); }

    private void handleTap(float x, float y) {
        float sx = getWidth() / 180f, sy = getHeight() / 580f, best = 34 * d;
        int hit = -1;
        for (int i = 1; i <= 10; i++) {
            float dist = (float) Math.hypot(x - XY[i][0] * sx, y - XY[i][1] * sy);
            if (dist < best) { best = dist; hit = i; }
        }
        if (hit > 0 && hit <= maxUnlocked && listener != null) listener.onStageTapped(hit);
    }

    private void group(Canvas c, List<PixelArt.R> r, float ox, float oy) { PixelArt.draw(c, paint, r, ox, oy, false, 0); }

    private void fillRect(Canvas c, int x, int y, int w, int h, int color) {
        paint.setColor(color); paint.setAlpha(255);
        c.drawRect(x, y, x + w, y + h, paint);
    }

    @Override
    protected void onDraw(Canvas c) {
        float sx = getWidth() / 180f, sy = getHeight() / 580f;
        c.save();
        c.scale(sx, sy);
        PixelArt.draw(c, paint, BASE);
        for (int[] s : ROAD) fillRect(c, s[0] - 2, s[1] - 2, s[2] + 4, s[3] + 4, 0xFF0D0F17);
        for (int[] s : ROAD) fillRect(c, s[0], s[1], s[2], s[3], s[1] < 140 ? 0xFF524048 : s[1] < 336 ? 0xFF49526B : 0xFF7A5D3B);
        for (int i = 1; i <= 10; i++) {
            group(c, SPECKLE_A, XY[i][0] - 3, XY[i][1] + 12);
            group(c, SPECKLE_B, XY[i][0] + 2, XY[i][1] - 14);
        }
        PixelArt.draw(c, paint, BRIDGE);
        group(c, INN, 144, 514);
        group(c, BANNER, 148, 286);
        group(c, GATE, 64, 4);
        for (int[] t : TREES) group(c, TREE, t[0], t[1]);
        group(c, TOMB1, 88, 192);
        group(c, TOMB2, 148, 166);
        c.restore();

        for (int i = 1; i <= 10; i++) if (i != selected) drawNode(c, i, sx, sy);
        drawNode(c, selected, sx, sy);
    }

    private void drawNode(Canvas c, int stage, float sx, float sy) {
        GameData.Stage st = GameData.stage(stage);
        boolean unlocked = stage <= maxUnlocked, isCleared = cleared[stage], sel = stage == selected, boss = st.isBoss;
        float cx = XY[stage][0] * sx, cy = XY[stage][1] * sy;
        float size = (boss ? 48 : 44) * d * (sel ? 1.1f : 1f), half = size / 2, b = 3 * d;

        int fill, numColor;
        if (!unlocked) { fill = 0xFF2A3142; numColor = 0xFF94A3B8; }
        else if (sel) { fill = boss ? 0xFFE11D48 : 0xFFF59E0B; numColor = boss ? 0xFFFFFFFF : 0xFF020617; }
        else if (isCleared) { fill = 0xFF059669; numColor = 0xFFFFFFFF; }
        else if (boss) { fill = 0xFFBE123C; numColor = 0xFFFFFFFF; }
        else { fill = 0xFF2C3859; numColor = 0xFFFDE68A; }

        // bóng + viền ngoài + mặt ô
        rect(c, cx - half, cy - half + 4 * d, cx + half, cy + half + 4 * d, PixelUi.OUTLINE);
        if (sel) rect(c, cx - half - b, cy - half - b, cx + half + b, cy + half + b, PixelUi.OUTLINE);
        rect(c, cx - half, cy - half, cx + half, cy + half, sel ? 0xFFFCD34D : PixelUi.OUTLINE);
        rect(c, cx - half + b, cy - half + b, cx + half - b, cy + half - b, fill);

        text.setTypeface(PixelUi.font);
        text.setColor(numColor);
        text.setTextSize((boss ? 18 : 16) * d);
        c.drawText(String.valueOf(stage), cx, cy + text.getTextSize() * 0.35f, text);

        // huy hiệu hoàn thành / khoá
        if (isCleared || !unlocked) {
            float bs = 18 * d, bx = cx + half - bs + 6 * d, by = cy + half - bs + 6 * d;
            // ✓: nền tối + viền vàng + dấu ✓ vàng để nổi bật trên ô màu xanh lá
            rect(c, bx - 2 * d, by - 2 * d, bx + bs + 2 * d, by + bs + 2 * d, isCleared ? 0xFFFDE047 : PixelUi.OUTLINE);
            rect(c, bx, by, bx + bs, by + bs, isCleared ? 0xFF0F172A : 0xFF1E293B);
            if (isCleared) PixelArt.drawIcon(c, paint, "check", bx + 2 * d, by + 2 * d, bs - 4 * d, true, 0xFFFDE047);
            else PixelArt.drawIcon(c, paint, "lock", bx + 2 * d, by + 2 * d, bs - 4 * d);
        }

        // nhãn BOSS phía trên
        if (boss) {
            float lw = 46 * d, lh = 18 * d, ly = cy - half - lh - 4 * d;
            rect(c, cx - lw / 2 - 2 * d, ly - 2 * d, cx + lw / 2 + 2 * d, ly + lh + 2 * d, PixelUi.OUTLINE);
            rect(c, cx - lw / 2, ly, cx + lw / 2, ly + lh, 0xFFE11D48);
            PixelArt.drawIcon(c, paint, "crown", cx - lw / 2 + 4 * d, ly + 3 * d, 12 * d);
            text.setColor(0xFFFFFFFF); text.setTextSize(10 * d);
            c.drawText("BOSS", cx + 8 * d, ly + lh * 0.7f, text);
        }

        // bảng tên bên dưới
        String label = boss ? "BOSS: " + st.name : "Ván " + stage;
        text.setTextSize(10 * d);
        Rect tb = new Rect();
        text.getTextBounds(label, 0, label.length(), tb);
        float pw = tb.width() + 12 * d, ph = 16 * d, py = cy + half + 6 * d;
        rect(c, cx - pw / 2 - d, py - d, cx + pw / 2 + d, py + ph + d, PixelUi.OUTLINE);
        rect(c, cx - pw / 2, py, cx + pw / 2, py + ph, sel ? 0xFFFCD34D : boss ? 0xFF2C1520 : 0xE6101422);
        text.setColor(sel ? 0xFF020617 : boss ? 0xFFFECDD3 : 0xFFE2E8F0);
        c.drawText(label, cx, py + ph * 0.72f, text);
    }

    private void rect(Canvas c, float l, float t, float r, float b, int color) {
        paint.setColor(color); paint.setAlpha(Color_alpha(color));
        c.drawRect(l, t, r, b, paint);
    }

    private static int Color_alpha(int color) { return (color >>> 24) & 0xFF; }
}
