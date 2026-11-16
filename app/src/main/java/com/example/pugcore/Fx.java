package com.example.pugcore;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Hiệu ứng pixel: lắc khi bị đánh, lao tới, nhấp nhô, chữ bay lên (port từ index.css). */
public final class Fx {
    private Fx() {}

    private static float dp(View v, float x) { return x * v.getResources().getDisplayMetrics().density; }

    /** Nhấp nhô 2 nấc như animate-idle-bob. Trả về animator để caller huỷ. */
    public static ObjectAnimator bob(View v) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "translationY", 0f, -dp(v, 4));
        a.setDuration(650);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(t -> t < 0.5f ? 0f : 1f);
        a.start();
        return a;
    }

    public static void shake(View v) {
        float d = dp(v, 6);
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "translationX", 0f, -d, d, -d * 0.66f, 0f);
        a.setDuration(220);
        a.start();
    }

    public static void lungeUp(View v) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "translationY", 0f, -dp(v, 28), 0f);
        a.setDuration(260);
        a.start();
    }

    public static void lungeDown(View v) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "translationY", 0f, dp(v, 26), 0f);
        a.setDuration(280);
        a.start();
    }

    /** Chữ nổi bay lên rồi mờ dần (+1 EXP, -dmg, +HP...). xFrac/yFrac là vị trí tương đối trong parent. */
    public static void popup(FrameLayout parent, String text, int color, float sp, float xFrac, float yFrac) {
        TextView tv = new TextView(parent.getContext());
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(sp);
        tv.setTypeface(PixelUi.font);
        tv.setShadowLayer(1f, 0f, dp(parent, 2), PixelUi.OUTLINE);
        tv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START));
        parent.addView(tv);
        tv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        float x = parent.getWidth() * xFrac - tv.getMeasuredWidth() / 2f;
        float y = parent.getHeight() * yFrac;
        tv.setTranslationX(x);
        tv.setTranslationY(y);
        tv.animate().translationY(y - dp(parent, 44)).alpha(0f).setDuration(680)
                .setListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) { parent.removeView(tv); }
                }).start();
    }

    /** Chữ đổi qua lại giữa 2 màu theo từng nấc (hiệu ứng lóe cho đồ Sử thi/Huyền thoại). Caller huỷ khi xong. */
    public static ValueAnimator blinkText(TextView tv, int colorA, int colorB) {
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(520);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setInterpolator(t -> t < 0.5f ? 0f : 1f);
        va.addUpdateListener(a -> tv.setTextColor(((float) a.getAnimatedValue()) < 0.5f ? colorA : colorB));
        va.start();
        return va;
    }

    /** Nháy sáng toàn màn hình theo nấc `times` lần (alpha 0.8 <-> 0). */
    public static void flash(View overlay, int color, int times) {
        overlay.setBackgroundColor(color);
        overlay.setVisibility(View.VISIBLE);
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(260L * times);
        va.setInterpolator(x -> {
            float f = (x * times) % 1f;
            return f < 0.45f ? 1f : 0f;
        });
        va.addUpdateListener(a -> overlay.setAlpha(0.8f * (float) a.getAnimatedValue()));
        va.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                overlay.setAlpha(0f);
                overlay.setVisibility(View.GONE);
            }
        });
        va.start();
    }
}
