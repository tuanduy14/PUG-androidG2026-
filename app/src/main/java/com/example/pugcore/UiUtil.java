package com.example.pugcore;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.view.OrientationEventListener;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

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

    /** Xoay ngang rồi vào bài tập dùng cánh tay (hít đất, chiến đấu). */
    public static void askRotateThen(Activity activity, Runnable start) {
        askRotateThen(activity, "thấy rõ vai và cánh tay của bạn", start);
    }

    /**
     * Tập luyện và Chiến đấu chạy ở chế độ ngang. Hiện thông báo yêu cầu xoay điện thoại;
     * khi cảm biến thấy máy đã nằm ngang thì tự vào luôn, hoặc bấm "Tiếp tục" để vào ngay.
     * `hint` mô tả vị trí đặt máy (vd. "thấy rõ vai, hông và cổ chân của bạn" cho Plank).
     */
    public static void askRotateThen(Activity activity, String hint, Runnable start) {
        final boolean[] started = {false};
        final OrientationEventListener[] listener = new OrientationEventListener[1];

        final PixelDialog dialog = new PixelDialog(activity)
                .title("XOAY NGANG ĐIỆN THOẠI", PixelUi.AMBER_LIGHT)
                .message("Hãy xoay điện thoại sang chiều ngang rồi đặt máy sao cho camera "
                        + hint + ".\n\nApp sẽ tự vào khi máy nằm ngang, hoặc bấm TIẾP TỤC.");

        Runnable go = () -> {
            if (started[0]) return;
            started[0] = true;
            if (listener[0] != null) listener[0].disable();
            dialog.dismiss();
            start.run();
        };
        dialog.button("TIẾP TỤC", PixelUi.AMBER, PixelUi.DARK_TEXT, go)
                .button("HỦY", 0xFF2A334D, PixelUi.TEXT, null);
        dialog.setOnDismissListener(d -> {
            if (listener[0] != null) listener[0].disable();
        });

        listener[0] = new OrientationEventListener(activity) {
            @Override
            public void onOrientationChanged(int o) {
                if (o == ORIENTATION_UNKNOWN) return;
                if ((o >= 60 && o <= 120) || (o >= 240 && o <= 300)) go.run();
            }
        };
        dialog.show();
        if (listener[0].canDetectOrientation()) listener[0].enable();
    }
}
