package com.example.pugcore;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;

public final class UiUtil {
    private UiUtil() {}

    public static boolean isDebuggable(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    /**
     * Màn hình camera tràn viền: camera vẽ dưới status bar / thanh điều hướng,
     * chỉ đẩy các lớp phủ (chữ, nút) ra khỏi vùng đó.
     */
    public static void applyOverlayInsets(View root, View topOverlay, View bottomOverlay) {
        final int tl = topOverlay.getPaddingLeft(), tt = topOverlay.getPaddingTop();
        final int tr = topOverlay.getPaddingRight(), tb = topOverlay.getPaddingBottom();
        final int bl = bottomOverlay.getPaddingLeft(), bt = bottomOverlay.getPaddingTop();
        final int br = bottomOverlay.getPaddingRight(), bb = bottomOverlay.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            topOverlay.setPadding(tl + bars.left, tt + bars.top, tr + bars.right, tb);
            bottomOverlay.setPadding(bl + bars.left, bt, br + bars.right, bb + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        // Icon trạng thái màu sáng cho dễ nhìn trên nền camera tối
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(
                ((Activity) root.getContext()).getWindow(),
                ((Activity) root.getContext()).getWindow().getDecorView());
        c.setAppearanceLightStatusBars(false);
        c.setAppearanceLightNavigationBars(false);
    }

    /** targetSdk 35+ bắt buộc edge-to-edge: chừa chỗ cho status bar / thanh điều hướng. */
    public static void applySystemBarPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
