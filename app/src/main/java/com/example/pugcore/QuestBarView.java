package com.example.pugcore;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;


public class QuestBarView extends View {
    private final Paint p = new Paint();
    private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d;
    private long dailyMs = 0;
    private int claimed = 0;

    public QuestBarView(Context c) { this(c, null); }

    public QuestBarView(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        p.setAntiAlias(false);
        t.setTextAlign(Paint.Align.CENTER);
    }

    public void setProgress(long dailyMs, int claimedMask) {
        this.dailyMs = dailyMs;
        this.claimed = claimedMask;
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), (int) (56 * d));
    }

    private void rect(Canvas c, float l, float tp, float r, float b, int color) {
        p.setColor(color);
        c.drawRect(l, tp, r, b, p);
    }

    private static String fmt(long sec) {
        return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), pad = 18 * d, b = 2 * d;
        float l = pad, r = w - pad, top = 3 * d, bot = 17 * d;
        float frac = Math.min(1f, dailyMs / 1000f / PlayerData.PLANK_GOAL_S);

        rect(c, l, top, r, bot, 0xFF05070C);
        rect(c, l + b, top + b, r - b, bot - b, 0xFF0D101A);
        float fx = l + b + (r - l - 2 * b) * frac;
        if (fx > l + b) {
            rect(c, l + b, top + b, fx, bot - b, 0xFFF59E0B);
            rect(c, l + b, top + b, fx, top + b + 2 * d, 0x4DFFFFFF);
        }

        t.setTypeface(PixelUi.font);
        t.setTextSize(10 * d);
        for (int i = 0; i < PlayerData.PLANK_MILESTONE_S.length; i++) {
            float x = l + (r - l) * (PlayerData.PLANK_MILESTONE_S[i] / (float) PlayerData.PLANK_GOAL_S);
            boolean got = (claimed >> i & 1) != 0;
            float m = 6 * d, cy = (top + bot) / 2f;
            rect(c, x - m - d, cy - m - d, x + m + d, cy + m + d, PixelUi.OUTLINE);
            rect(c, x - m, cy - m, x + m, cy + m, got ? 0xFFFBBF24 : 0xFF334155);
            if (got) rect(c, x - m, cy - m, x + m, cy - m + 2 * d, 0x66FFFFFF);

            String time = fmt(PlayerData.PLANK_MILESTONE_S[i]);
            String reward = "+" + PlayerData.PLANK_MILESTONE_TICKETS[i] + " VÉ";
            float half = Math.max(t.measureText(time), t.measureText(reward)) / 2f;
            float tx = Math.max(half, Math.min(w - half, x));
            t.setColor(got ? 0xFFFDE68A : 0xFF94A3B8);
            c.drawText(time, tx, bot + 15 * d, t);
            t.setColor(got ? 0xFFFBBF24 : 0xFF64748B);
            c.drawText(reward, tx, bot + 28 * d, t);
        }
    }
}
