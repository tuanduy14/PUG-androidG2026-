package com.example.pugcore;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Âm thanh 8-bit kiểu game cổ điển, tự tổng hợp bằng code (port từ utils/sound.ts của bản mẫu),
 * không cần file âm thanh. Gọi SoundFx.init(context) ở mỗi Activity rồi SoundFx.play(SoundFx.XXX).
 */
public final class SoundFx {
    private SoundFx() {}

    public static final int TRAIN_CLICK = 0, TAP_ATTACK = 1, HEAVY_STRIKE = 2, MONSTER_HIT = 3, LEVEL_UP = 4,
            VICTORY = 5, GAME_OVER = 6, BUY = 7, POTION = 8, TAB = 9,
            GACHA_PULL = 10, GACHA_EPIC = 11, GACHA_LEGEND = 12, TICKET = 13;
    private static final int COUNT = 14;
    static final int RATE = 22050;
    private static final float MASTER_GAIN = 3.0f; // bản web để volume rất nhỏ, trên điện thoại cần to hơn

    private static SoundPool pool;
    private static final int[] ids = new int[COUNT];
    private static int musicVol = 50, sfxVol = 80; // 0..100
    private static SharedPreferences prefs;

    public static synchronized void init(Context context) {
        if (pool != null) return;
        Context app = context.getApplicationContext();
        prefs = app.getSharedPreferences("pug_sound", Context.MODE_PRIVATE);
        boolean wasMuted = prefs.getBoolean("muted", false); // bản cũ chỉ có nút tắt/bật
        musicVol = prefs.getInt("music", wasMuted ? 0 : 50);
        sfxVol = prefs.getInt("sfx", wasMuted ? 0 : 80);

        pool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();

        for (int i = 0; i < COUNT; i++) {
            try {
                File f = new File(app.getCacheDir(), "sfx_" + i + ".wav");
                try (FileOutputStream out = new FileOutputStream(f)) {
                    out.write(wav(render(i)));
                }
                ids[i] = pool.load(f.getAbsolutePath(), 1);
            } catch (IOException ignored) {
                ids[i] = 0;
            }
        }
    }

    public static void play(int fx) {
        if (sfxVol <= 0 || pool == null || fx < 0 || fx >= COUNT || ids[fx] == 0) return;
        float v = sfxVol / 100f;
        pool.play(ids[fx], v, v, 1, 0, 1f);
    }

    public static int getMusicVolume() { return musicVol; }
    public static int getSfxVolume() { return sfxVol; }

    public static void setMusicVolume(int v) {
        musicVol = Math.max(0, Math.min(100, v));
        if (prefs != null) prefs.edit().putInt("music", musicVol).apply();
    }

    public static void setSfxVolume(int v) {
        sfxVol = Math.max(0, Math.min(100, v));
        if (prefs != null) prefs.edit().putInt("sfx", sfxVol).apply();
    }

    // ------------------------------------------------------------------ tổng hợp

    private static final int SQUARE = 0, TRIANGLE = 1, SAW = 2, SINE = 3;

    private static final class Tone {
        final double freq, dur, vol, delay, slideTo; final int type;
        Tone(double freq, double dur, int type, double vol, double delay, double slideTo) {
            this.freq = freq; this.dur = dur; this.type = type; this.vol = vol; this.delay = delay; this.slideTo = slideTo;
        }
    }

    private static Tone t(double f, double d, int type, double v, double delay, double slide) {
        return new Tone(f, d, type, v, delay, slide);
    }

    private static Tone[] tones(int fx) {
        switch (fx) {
            case TRAIN_CLICK: return new Tone[]{t(340, .06, SQUARE, .06, 0, 520)};
            case TAP_ATTACK: return new Tone[]{t(460, .045, TRIANGLE, .08, 0, 220)};
            case HEAVY_STRIKE: return new Tone[]{t(190, .14, SAW, .11, 0, 65), t(580, .09, SQUARE, .07, .02, 240)};
            case MONSTER_HIT: return new Tone[]{t(150, .18, SAW, .10, 0, 55)};
            case LEVEL_UP: {
                double[] n = {523.25, 659.25, 783.99, 1046.5};
                Tone[] r = new Tone[4];
                for (int i = 0; i < 4; i++) r[i] = t(n[i], .12, SQUARE, .075, i * .07, 0);
                return r;
            }
            case VICTORY: {
                double[] n = {440, 554.37, 659.25, 880};
                Tone[] r = new Tone[4];
                for (int i = 0; i < 4; i++) r[i] = t(n[i], i == 3 ? .28 : .1, SQUARE, .08, i * .08, 0);
                return r;
            }
            case GAME_OVER: {
                double[] n = {330, 293.66, 261.63, 196};
                Tone[] r = new Tone[4];
                for (int i = 0; i < 4; i++) r[i] = t(n[i], i == 3 ? .35 : .14, SAW, .075, i * .12, 0);
                return r;
            }
            case BUY: return new Tone[]{t(587.33, .07, SQUARE, .07, 0, 0), t(880, .12, SQUARE, .07, .06, 0)};
            case POTION: return new Tone[]{t(392, .09, SINE, .09, 0, 587), t(587, .14, SINE, .09, .08, 784)};
            case GACHA_PULL: {
                double[] n = {392, 523.25, 659.25, 783.99};
                Tone[] r = new Tone[4];
                for (int i = 0; i < 4; i++) r[i] = t(n[i], .08, SQUARE, .07, i * .06, 0);
                return r;
            }
            case GACHA_EPIC: {
                double[] n = {523.25, 659.25, 783.99, 1046.5, 1318.5};
                Tone[] r = new Tone[6];
                for (int i = 0; i < 5; i++) r[i] = t(n[i], .1, SQUARE, .08, i * .07, 0);
                r[5] = t(1318.5, .3, SINE, .09, .35, 0);
                return r;
            }
            case GACHA_LEGEND: {
                double[] n = {523.25, 659.25, 783.99, 1046.5, 1318.5, 1568};
                Tone[] r = new Tone[8];
                for (int i = 0; i < 6; i++) r[i] = t(n[i], .11, SQUARE, .08, i * .07, 0);
                r[6] = t(1046.5, .5, SINE, .09, .42, 0);
                r[7] = t(1568, .5, TRIANGLE, .07, .42, 0);
                return r;
            }
            case TICKET: return new Tone[]{t(880, .07, SQUARE, .07, 0, 0), t(1318.5, .12, SQUARE, .07, .07, 0)};
            default: return new Tone[]{t(420, .04, TRIANGLE, .045, 0, 0)}; // TAB
        }
    }

    private static short[] render(int fx) {
        Tone[] ts = tones(fx);
        double total = 0;
        for (Tone x : ts) total = Math.max(total, x.delay + x.dur);
        int n = (int) ((total + 0.05) * RATE);
        float[] mix = new float[n];

        for (Tone x : ts) {
            int start = (int) (x.delay * RATE), len = (int) (x.dur * RATE);
            double phase = 0;
            for (int i = 0; i < len && start + i < n; i++) {
                double time = i / (double) RATE;
                double f = x.slideTo > 0 ? x.freq * Math.pow(x.slideTo / x.freq, time / x.dur) : x.freq;
                phase += 2 * Math.PI * f / RATE;
                double env = time < 0.01 ? x.vol * time / 0.01
                        : x.vol * Math.pow(0.0001 / x.vol, (time - 0.01) / Math.max(0.001, x.dur - 0.01));
                double s;
                switch (x.type) {
                    case SQUARE: s = Math.sin(phase) >= 0 ? 1 : -1; break;
                    case TRIANGLE: s = 2 / Math.PI * Math.asin(Math.sin(phase)); break;
                    case SAW: { double c = phase / (2 * Math.PI); s = 2 * (c - Math.floor(c + 0.5)); break; }
                    default: s = Math.sin(phase);
                }
                mix[start + i] += (float) (s * env);
            }
        }
        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = Math.max(-1f, Math.min(1f, mix[i] * MASTER_GAIN));
            pcm[i] = (short) (v * 32767);
        }
        return pcm;
    }

    static byte[] wav(short[] pcm) throws IOException {
        int dataLen = pcm.length * 2;
        ByteArrayOutputStream o = new ByteArrayOutputStream(44 + dataLen);
        o.write("RIFF".getBytes());
        le(o, 36 + dataLen, 4);
        o.write("WAVEfmt ".getBytes());
        le(o, 16, 4); le(o, 1, 2); le(o, 1, 2);
        le(o, RATE, 4); le(o, RATE * 2, 4); le(o, 2, 2); le(o, 16, 2);
        o.write("data".getBytes());
        le(o, dataLen, 4);
        for (short s : pcm) le(o, s, 2);
        return o.toByteArray();
    }

    private static void le(ByteArrayOutputStream o, int v, int bytes) {
        for (int i = 0; i < bytes; i++) o.write((v >> (8 * i)) & 0xFF);
    }
}
