package com.example.pugcore;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Hộp thoại kiểu pixel dùng chung cho toàn game: khung viền dày có bóng đổ, thanh tiêu đề, nút 3D.
 * Dùng: new PixelDialog(activity).title("...", màu).message("...").button("OK", ...).show();
 */
public class PixelDialog extends Dialog {

    /** Hàng lựa chọn (thay RadioButton). */
    public static final class Option {
        public final LinearLayout row;
        private final View check;
        private final Context ctx;

        Option(Context ctx, LinearLayout row, View check) { this.ctx = ctx; this.row = row; this.check = check; }

        public void setChecked(boolean on) {
            row.setBackground(PixelUi.box(ctx, on ? 0xFF252F4A : 0xFF161C2C, on ? PixelUi.AMBER : PixelUi.OUTLINE, 3));
            check.setBackground(PixelUi.box(ctx, on ? PixelUi.AMBER : 0xFF0D111C, on ? PixelUi.OUTLINE : 0xFF475569, 2));
        }
    }

    /** Hàng thanh trượt có nhãn và giá trị. */
    public static final class SliderRow {
        public final LinearLayout row;
        public final PixelSliderView slider;

        SliderRow(LinearLayout row, PixelSliderView slider) { this.row = row; this.slider = slider; }

        public void setEnabled(boolean e) {
            slider.setEnabled(e);
            row.setAlpha(e ? 1f : 0.45f);
        }
    }

    private final Context ctx;
    private final float d;
    private final FrameLayout root;
    private final TextView tvTitle;
    private final LinearLayout body, buttons;
    private final View buttonDivider;

    public PixelDialog(Activity a) {
        super(a);
        ctx = a;
        d = a.getResources().getDisplayMetrics().density;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        root = new FrameLayout(a);
        int m = (int) (8 * d);
        root.setPadding(m, m, m, m);

        LinearLayout panel = new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(panelBg());
        panel.setPadding((int) (3 * d), (int) (3 * d), (int) (3 * d), (int) (7 * d));
        root.addView(panel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tvTitle = new TextView(a);
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setTextSize(17);
        tvTitle.setTextColor(PixelUi.AMBER_LIGHT);
        tvTitle.setBackgroundColor(0xFF161C2C);
        tvTitle.setPadding((int) (12 * d), (int) (12 * d), (int) (12 * d), (int) (12 * d));
        panel.addView(tvTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(divider(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (3 * d)));

        ScrollView sv = new ScrollView(a);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        sv.setVerticalScrollBarEnabled(false);
        body = new LinearLayout(a);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding((int) (16 * d), (int) (14 * d), (int) (16 * d), (int) (6 * d));
        sv.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        buttonDivider = divider();
        panel.addView(buttonDivider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (3 * d)));
        buttons = new LinearLayout(a);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setBackgroundColor(0xFF161C2C);
        buttons.setPadding((int) (8 * d), (int) (10 * d), (int) (8 * d), (int) (6 * d));
        panel.addView(buttons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private View divider() {
        View v = new View(ctx);
        v.setBackgroundColor(PixelUi.OUTLINE);
        return v;
    }

    private Drawable panelBg() {
        GradientDrawable shadow = new GradientDrawable();
        shadow.setColor(0xFF05070C);
        GradientDrawable face = new GradientDrawable();
        face.setColor(0xFF1C2233);
        face.setStroke((int) (3 * d), PixelUi.OUTLINE);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{shadow, face});
        ld.setLayerInset(1, 0, 0, 0, (int) (4 * d));
        return ld;
    }

    // ------------------------------------------------------------------ nội dung

    public PixelDialog title(String text, int color) {
        tvTitle.setText(text);
        tvTitle.setTextColor(color);
        return this;
    }

    public TextView titleView() { return tvTitle; }

    public PixelDialog message(String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFFCBD5E1);
        tv.setLineSpacing(3 * d, 1f);
        return add(tv);
    }

    public PixelDialog add(View v) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        LinearLayout.LayoutParams l = lp instanceof LinearLayout.LayoutParams
                ? (LinearLayout.LayoutParams) lp
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        l.bottomMargin = (int) (10 * d);
        body.addView(v, l);
        return this;
    }

    public PixelDialog button(String text, int color, int textColor, Runnable onClick) {
        TextView b = new TextView(ctx);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(textColor);
        b.setGravity(Gravity.CENTER);
        b.setClickable(true);
        b.setFocusable(true);
        b.setBackground(PixelUi.button(ctx, color));
        b.setPadding((int) (10 * d), (int) (10 * d), (int) (10 * d), (int) (14 * d));
        b.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            dismiss();
            if (onClick != null) onClick.run();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = lp.rightMargin = (int) (4 * d);
        buttons.addView(b, lp);
        return this;
    }

    public PixelDialog noCancel() {
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        return this;
    }

    /** Hàng lựa chọn kiểu ô vuông: tiêu đề + mô tả nhỏ. */
    public Option option(String title, String subtitle, boolean checked, Runnable onClick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding((int) (10 * d), (int) (10 * d), (int) (10 * d), (int) (10 * d));

        View check = new View(ctx);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams((int) (20 * d), (int) (20 * d));
        cl.rightMargin = (int) (10 * d);
        row.addView(check, cl);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(13);
        t.setTextColor(PixelUi.TEXT);
        col.addView(t);
        TextView s = new TextView(ctx);
        s.setText(subtitle);
        s.setTextSize(11);
        s.setTextColor(PixelUi.SLATE);
        col.addView(s);
        row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Option o = new Option(ctx, row, check);
        o.setChecked(checked);
        row.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            onClick.run();
        });
        return o;
    }

    /** Hàng thanh trượt: nhãn bên trái, giá trị bên phải. */
    public SliderRow slider(String label, int min, int max, int value, String unit, int color,
                            PixelSliderView.Listener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = new TextView(ctx);
        name.setText(label);
        name.setTextSize(13);
        name.setTextColor(PixelUi.TEXT);
        head.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView val = new TextView(ctx);
        val.setText(value + unit);
        val.setTextSize(13);
        val.setTextColor(color);
        head.addView(val);
        row.addView(head);

        PixelSliderView sl = new PixelSliderView(ctx);
        sl.setRange(min, max);
        sl.setValue(value);
        sl.setFillColor(color);
        sl.setListener(new PixelSliderView.Listener() {
            @Override public void onChanged(int v, boolean fromUser) {
                val.setText(v + unit);
                listener.onChanged(v, fromUser);
            }
            @Override public void onStop(int v) { listener.onStop(v); }
        });
        row.addView(sl, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new SliderRow(row, sl);
    }

    @Override
    public void show() {
        PixelUi.applyFont(root);
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            int width = (int) Math.min(400 * d, dm.widthPixels * 0.92f);
            w.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setDimAmount(0.7f);
        }
        super.show();
    }
}
