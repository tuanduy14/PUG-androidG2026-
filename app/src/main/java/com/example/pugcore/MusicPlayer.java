package com.example.pugcore;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Nhạc nền chiptune 8-bit tự tổng hợp bằng code (không cần file nhạc), lặp vô hạn.
 * 3 bản: MENU (menu/bản đồ/cửa hàng), TRAIN (tập luyện), BATTLE (chiến đấu).
 * Mỗi Activity gọi play() ở onResume và pause() ở onPause. Cùng bản nhạc thì phát tiếp, không phát lại từ đầu.
 * Âm lượng nhạc nền chỉnh trong hộp thoại Cài đặt (nút bánh răng), lưu trong SoundFx.
 */
public final class MusicPlayer {
    private MusicPlayer() {}

    public static final int MENU = 0, TRAIN = 1, BATTLE = 2;
    private static final String VERSION = "v1";    // đổi khi sửa bản nhạc để tạo lại file

    /** 0..100% -> âm lượng MediaPlayer; 50% = 0.35 như mặc định cũ (nhỏ hơn hiệu ứng để không át tiếng đánh). */
    private static float volume() { return SoundFx.getMusicVolume() / 100f * 0.7f; }

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static Context app;
    private static MediaPlayer mp;
    private static int current = -1, wanted = MENU;
    private static boolean active = false;
    private static boolean generating = false;

    public static synchronized void play(Context c, int track) {
        app = c.getApplicationContext();
        wanted = track;
        active = true;
        startWanted();
    }

    public static synchronized void pause() {
        active = false;
        if (mp != null && mp.isPlaying()) mp.pause();
    }

    /** Gọi sau khi đổi âm lượng nhạc nền: áp dụng ngay, về 0 thì tạm dừng. */
    public static synchronized void refresh() {
        if (!active) return;
        if (SoundFx.getMusicVolume() <= 0) {
            if (mp != null && mp.isPlaying()) mp.pause();
            return;
        }
        if (mp != null && current == wanted) {
            mp.setVolume(volume(), volume());
            if (!mp.isPlaying()) mp.start();
        } else {
            startWanted();
        }
    }

    private static void startWanted() {
        if (app == null || SoundFx.getMusicVolume() <= 0) {
            if (mp != null && mp.isPlaying()) mp.pause();
            return;
        }
        if (mp != null && current == wanted) {
            mp.setVolume(volume(), volume());
            if (!mp.isPlaying()) mp.start();
            return;
        }
        release();
        final File f = file(wanted);
        if (f.exists()) {
            try {
                MediaPlayer p = new MediaPlayer();
                p.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                p.setDataSource(f.getAbsolutePath());
                p.setLooping(true);
                p.setVolume(volume(), volume());
                p.prepare();
                p.start();
                mp = p;
                current = wanted;
            } catch (Exception e) {
                release();
            }
        } else if (!generating) {
            generating = true;
            final int track = wanted;
            new Thread(() -> {
                try {
                    short[] pcm = render(track);
                    try (FileOutputStream out = new FileOutputStream(file(track))) {
                        out.write(SoundFx.wav(pcm));
                    }
                } catch (Exception ignored) { }
                main.post(() -> {
                    synchronized (MusicPlayer.class) {
                        generating = false;
                        if (active) startWanted();
                    }
                });
            }).start();
        }
    }

    private static void release() {
        if (mp != null) {
            try { mp.release(); } catch (Exception ignored) { }
            mp = null;
        }
        current = -1;
    }

    private static File file(int track) {
        return new File(app.getCacheDir(), "music_" + VERSION + "_" + track + ".wav");
    }

    // ------------------------------------------------------------------ bản nhạc

    private static String bars(String pattern, String... roots) {
        StringBuilder sb = new StringBuilder();
        for (String r : roots) sb.append(pattern.replace("X", r)).append(' ');
        return sb.toString();
    }

    private static final String MENU_LEAD =
            "C5:1 E5:.5 G5:.5 E5:1 C5:1  A4:1 C5:.5 E5:.5 C5:1 A4:1  F4:1 A4:.5 C5:.5 A4:1 F4:1  G4:1 B4:.5 D5:.5 B4:1 G4:1 "
          + "E5:1 G5:1 C6:1 G5:1  C6:1 B5:.5 A5:.5 G5:2  F5:1 A5:1 C6:1 A5:1  D5:1 G5:1 B5:1 D6:1";
    private static final String TRAIN_A =
            "A4:.5 C5:.5 E5:.5 C5:.5 A4:.5 C5:.5 E5:.5 A5:.5  F4:.5 A4:.5 C5:.5 A4:.5 F4:.5 A4:.5 C5:.5 F5:.5  "
          + "C5:.5 E5:.5 G5:.5 E5:.5 C5:.5 E5:.5 G5:.5 C6:.5  G4:.5 B4:.5 D5:.5 B4:.5 G4:.5 B4:.5 D5:.5 G5:.5 ";
    private static final String BATTLE_LEAD =
            "D5:.5 -:.5 D5:.5 F5:.5 A5:1 F5:.5 D5:.5  Bb4:.5 -:.5 Bb4:.5 D5:.5 F5:1 D5:.5 Bb4:.5  "
          + "C5:.5 -:.5 C5:.5 E5:.5 G5:1 E5:.5 C5:.5  A4:.5 C#5:.5 E5:.5 A5:.5 G5:.5 E5:.5 C#5:.5 A4:.5  "
          + "D6:.5 -:.5 D6:.5 C6:.5 A5:1 F5:.5 A5:.5  Bb5:.5 -:.5 Bb5:.5 A5:.5 F5:1 D5:.5 F5:.5  "
          + "C6:.5 -:.5 C6:.5 B5:.5 G5:1 E5:.5 G5:.5  A5:1 G5:.5 F5:.5 E5:1 C#5:1";

    /** {lead, bass, bpm} */
    private static Object[] song(int track) {
        switch (track) {
            case TRAIN:
                return new Object[]{TRAIN_A + TRAIN_A,
                        bars("X:1 X:1 X:1 X:1", "A2", "F2", "C3", "G2", "A2", "F2", "C3", "G2"), 128.0};
            case BATTLE:
                return new Object[]{BATTLE_LEAD,
                        bars("X:.5 X:.5 X:.5 X:.5 X:.5 X:.5 X:.5 X:.5", "D3", "Bb2", "C3", "A2", "D3", "Bb2", "C3", "A2"), 150.0};
            default:
                return new Object[]{MENU_LEAD,
                        bars("X:2 X:2", "C3", "A2", "F2", "G2", "C3", "A2", "F2", "G2"), 96.0};
        }
    }

    // ------------------------------------------------------------------ tổng hợp

    private static short[] render(int track) {
        Object[] s = song(track);
        double spb = 60.0 / (Double) s[2];
        int rate = SoundFx.RATE;
        float[] lead = voice((String) s[0], spb, rate, true, 0.16f);
        float[] bass = voice((String) s[1], spb, rate, false, 0.22f);
        int n = Math.max(lead.length, bass.length);
        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = (i < lead.length ? lead[i] : 0) + (i < bass.length ? bass[i] : 0);
            pcm[i] = (short) (Math.max(-1f, Math.min(1f, v)) * 32767);
        }
        return pcm;
    }

    private static float[] voice(String seq, double spb, int rate, boolean square, float vol) {
        String[] toks = seq.trim().split("\\s+");
        double beats = 0;
        for (String t : toks) beats += Double.parseDouble(t.substring(t.indexOf(':') + 1));
        float[] out = new float[(int) (beats * spb * rate) + 1];
        double pos = 0;
        for (String t : toks) {
            int c = t.indexOf(':');
            String name = t.substring(0, c);
            double dur = Double.parseDouble(t.substring(c + 1));
            int start = (int) (pos * spb * rate), len = (int) (dur * spb * rate);
            pos += dur;
            if (name.equals("-")) continue;
            double freq = 440.0 * Math.pow(2, (midi(name) - 69) / 12.0);
            double phase = 0, step = 2 * Math.PI * freq / rate;
            int fade = (int) (0.008 * rate);
            for (int i = 0; i < len && start + i < out.length; i++) {
                phase += step;
                double w = square ? (Math.sin(phase) >= 0 ? 1 : -1) : 2 / Math.PI * Math.asin(Math.sin(phase));
                double env = Math.min(1.0, i / (0.004 * rate)) * (1 - 0.35 * i / (double) len);
                if (i > len - fade) env *= Math.max(0, (len - i) / (double) fade);
                out[start + i] += (float) (w * env * vol);
            }
        }
        return out;
    }

    private static int midi(String name) {
        int idx = 0;
        int base;
        switch (name.charAt(idx++)) {
            case 'C': base = 0; break;
            case 'D': base = 2; break;
            case 'E': base = 4; break;
            case 'F': base = 5; break;
            case 'G': base = 7; break;
            case 'A': base = 9; break;
            default: base = 11; // B
        }
        if (name.charAt(idx) == '#') { base++; idx++; }
        else if (name.charAt(idx) == 'b') { base--; idx++; }
        int octave = Integer.parseInt(name.substring(idx));
        return 12 * (octave + 1) + base;
    }
}
