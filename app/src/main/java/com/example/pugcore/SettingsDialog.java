package com.example.pugcore;

import android.app.Activity;
import android.view.View;

/** Nút bánh răng nhỏ ở góc màn hình + hộp thoại cài đặt âm lượng (nhạc nền, hiệu ứng). */
public final class SettingsDialog {
    private SettingsDialog() {}

    /** Gắn nút bánh răng (R.id.btnSettings) của Activity: tạo kiểu pixel và mở hộp thoại khi bấm. */
    public static void attach(Activity a) {
        View b = a.findViewById(R.id.btnSettings);
        if (b == null) return;
        b.setBackground(PixelUi.button(a, 0xFF28314B));
        b.setOnClickListener(v -> {
            SoundFx.play(SoundFx.TAB);
            show(a);
        });
    }

    public static void show(Activity a) {
        PixelDialog d = new PixelDialog(a).title("CÀI ĐẶT", PixelUi.AMBER_LIGHT);

        PixelDialog.SliderRow music = d.slider("NHẠC NỀN", 0, 100, SoundFx.getMusicVolume(), "%", 0xFF38BDF8,
                (v, user) -> {
                    SoundFx.setMusicVolume(v);
                    MusicPlayer.refresh();
                });
        PixelDialog.SliderRow sfx = d.slider("HIỆU ỨNG ÂM THANH", 0, 100, SoundFx.getSfxVolume(), "%", 0xFFF59E0B,
                new PixelSliderView.Listener() {
                    @Override public void onChanged(int v, boolean user) { SoundFx.setSfxVolume(v); }
                    @Override public void onStop(int v) { SoundFx.play(SoundFx.TAP_ATTACK); } // nghe thử khi thả tay
                });

        d.add(music.row).add(sfx.row)
                .button("XONG", PixelUi.AMBER, PixelUi.DARK_TEXT, null)
                .show();
    }
}
