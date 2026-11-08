package com.example.pugcore;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;

/** Kết quả nhận diện của 1 khung hình, dùng để vẽ khung xương và hiện số liệu debug. */
public class PoseFrame {
    public final float[] x;          // tọa độ chuẩn hóa 0..1 theo ảnh phân tích
    public final float[] y;
    public final float[] visibility;
    public final int width;          // kích thước ảnh phân tích (đã xoay đúng chiều)
    public final int height;
    public final String arm;         // "trái" hoặc "phải" (tay đang được dùng để đếm)
    public final double angle;       // góc khuỷu đã làm mượt, NaN nếu chưa đo được
    public final boolean down;       // đã hạ người (góc < ngưỡng) và đang chờ duỗi
    public final boolean mirrored;   // hình preview đang hiện kiểu gương hay không

    PoseFrame(List<NormalizedLandmark> lm, int width, int height, String arm,
              double angle, boolean down, boolean mirrored) {
        int n = (lm == null) ? 0 : lm.size();
        x = new float[n];
        y = new float[n];
        visibility = new float[n];
        for (int i = 0; i < n; i++) {
            NormalizedLandmark l = lm.get(i);
            x[i] = l.x();
            y[i] = l.y();
            visibility[i] = l.visibility().orElse(0f);
        }
        this.width = width;
        this.height = height;
        this.arm = arm;
        this.angle = angle;
        this.down = down;
        this.mirrored = mirrored;
    }

    public boolean hasPerson() {
        return x.length > 0;
    }
}
