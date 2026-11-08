package com.example.pugcore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.PoseLandmarkerOptions;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Đóng gói camera + MediaPipe + thuật toán đếm push-up.
 * Chỉ phân tích khung hình khi setCounting(true).
 */
public class PoseTracker {

    public interface RepListener {
        void onRep(); // luôn được gọi trên main thread
    }

    public interface FrameListener {
        void onFrame(PoseFrame frame); // gọi trên main thread, mỗi khung hình được phân tích
    }

    private static final String TAG = "PoseTracker";
    private static final double DOWN_ANGLE = 90;   // góc khuỷu < 90 => đã hạ người
    private static final double UP_ANGLE = 160;    // góc khuỷu > 160 => đã đẩy lên
    private static final float MIN_VISIBILITY = 0.5f;
   
    private static final long MIN_FRAME_INTERVAL_MS = 100;

    private static final boolean FLIP_PREVIEW = true;

    private final Context context;
    private final RepListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private PoseLandmarker poseLandmarker;
    private volatile boolean counting = false;
    private volatile boolean resetPending = false;
    private volatile boolean released = false;
    private volatile boolean previewMirrored = false;
    private volatile FrameListener frameListener;

    private boolean isDown = false;
    private double smoothedAngle = -1;
    private long lastTimestamp = 0;
    private long lastAnalyzedAt = 0;

    public PoseTracker(Context context, RepListener listener) {
        this.context = context;
        this.listener = listener;
        setupPoseLandmarker();
    }

    private void setupPoseLandmarker() {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("pose_landmarker_lite.task")
                    .build();

            PoseLandmarkerOptions options = PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setMinPoseDetectionConfidence(0.5f)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener(this::processPoseResult)
                    .setErrorListener(error -> Log.e(TAG, "MediaPipe Error: " + error.getMessage()))
                    .build();

            poseLandmarker = PoseLandmarker.createFromOptions(context, options);
        } catch (Throwable e) { // UnsatisfiedLinkError là Error, không phải Exception
            Log.e(TAG, "Lỗi khởi tạo MediaPipe: " + e.getMessage(), e);
        }
    }

    public void setFrameListener(FrameListener l) {
        this.frameListener = l;
    }

    private void postFrame(PoseFrame frame) {
        FrameListener l = frameListener;
        if (l == null || released) return;
        mainHandler.post(() -> l.onFrame(frame));
    }


    public boolean isReady() {
        return poseLandmarker != null;
    }

    /** Bật/tắt đếm rep. Mỗi lần bật sẽ reset trạng thái lên/xuống. */
    public void setCounting(boolean on) {
        if (on) resetPending = true;
        counting = on;
    }

    public void startCamera(LifecycleOwner owner, PreviewView viewFinder) {

        viewFinder.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        viewFinder.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        viewFinder.setScaleX(FLIP_PREVIEW ? -1f : 1f);

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                int cameraCount = provider.getAvailableCameraInfos().size();
                Log.i(TAG, "Số camera khả dụng: " + cameraCount);

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());


                ImageAnalysis analysis = null;
                if (poseLandmarker != null) {
                    ResolutionSelector analysisRes = new ResolutionSelector.Builder()
                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                            .setResolutionStrategy(new ResolutionStrategy(new Size(640, 360),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
                            .build();
                    analysis = new ImageAnalysis.Builder()
                            .setResolutionSelector(analysisRes)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build();
                    analysis.setAnalyzer(executor, this::analyze);
                }


                if (bind(provider, owner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)) {
                    // camera trước: CameraX tự lật gương, FLIP_PREVIEW lật thêm 1 lần nữa
                    previewMirrored = !FLIP_PREVIEW;
                } else if (bind(provider, owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)) {
                    previewMirrored = FLIP_PREVIEW;
                } else {
                    Toast.makeText(context, "Không mở được camera (máy chỉ thấy " + cameraCount
                            + " camera). Xem Logcat tag PoseTracker", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khởi chạy Camera: " + e.getMessage(), e);
                Toast.makeText(context, "Lỗi camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private boolean bind(ProcessCameraProvider provider, LifecycleOwner owner,
                         CameraSelector selector, Preview preview, ImageAnalysis analysis) {
        try {
            provider.unbindAll();
            if (analysis != null) {
                provider.bindToLifecycle(owner, selector, preview, analysis);
            } else {
                provider.bindToLifecycle(owner, selector, preview);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Không bind được camera: " + e.getMessage());
            return false;
        }
    }

    private void analyze(@NonNull ImageProxy imageProxy) {
        try {
            if (released || poseLandmarker == null || !counting) return;
            long now = SystemClock.uptimeMillis();
            if (now - lastAnalyzedAt < MIN_FRAME_INTERVAL_MS) return; // bỏ bớt khung hình
            lastAnalyzedAt = now;

            Bitmap bitmap = imageProxy.toBitmap();
            Matrix matrix = new Matrix();
            matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());
            Bitmap rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            long ts = SystemClock.uptimeMillis();
            if (ts <= lastTimestamp) ts = lastTimestamp + 1; // timestamp phải tăng dần
            lastTimestamp = ts;

            poseLandmarker.detectAsync(new BitmapImageBuilder(rotated).build(), ts);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi phân tích khung hình: " + e.getMessage());
        } finally {
            imageProxy.close();
        }
    }

    private void processPoseResult(PoseLandmarkerResult result, MPImage inputImage) {
        if (!counting) return;
        int imgW = inputImage.getWidth(), imgH = inputImage.getHeight();

        if (result.landmarks().isEmpty()) {
            postFrame(new PoseFrame(null, imgW, imgH, "", Double.NaN, false, previewMirrored));
            return;
        }

        if (resetPending) {
            isDown = false;
            smoothedAngle = -1;
            resetPending = false;
        }

        List<NormalizedLandmark> lm = result.landmarks().get(0);

        // Chọn tay nào đang hiện rõ hơn: trái (11,13,15) hoặc phải (12,14,16)
        float left = armVisibility(lm, 11, 13, 15);
        float right = armVisibility(lm, 12, 14, 16);
        boolean useLeft = left >= right;
        int s = useLeft ? 11 : 12, e = useLeft ? 13 : 14, w = useLeft ? 15 : 16;
        float vis = useLeft ? left : right;

        double angle = Double.NaN;
        if (vis >= MIN_VISIBILITY) {
            double raw = calculateAngle(lm.get(s), lm.get(e), lm.get(w), imgW, imgH);
            smoothedAngle = (smoothedAngle < 0) ? raw : 0.5 * smoothedAngle + 0.5 * raw;
            angle = smoothedAngle;

            if (smoothedAngle < DOWN_ANGLE) {
                isDown = true;
            } else if (smoothedAngle > UP_ANGLE && isDown) {
                isDown = false;
                mainHandler.post(listener::onRep);
            }
        }

        postFrame(new PoseFrame(lm, imgW, imgH, useLeft ? "trái" : "phải",
                angle, isDown, previewMirrored));
    }

    private static float armVisibility(List<NormalizedLandmark> lm, int a, int b, int c) {
        return Math.min(vis(lm.get(a)), Math.min(vis(lm.get(b)), vis(lm.get(c))));
    }

    private static float vis(NormalizedLandmark l) {
        return l.visibility().orElse(0f);
    }

    /** Góc tại p2, đã nhân theo kích thước ảnh để không bị méo khi ảnh không vuông. */
    private static double calculateAngle(NormalizedLandmark p1, NormalizedLandmark p2,
                                         NormalizedLandmark p3, int width, int height) {
        double ax = (p1.x() - p2.x()) * width, ay = (p1.y() - p2.y()) * height;
        double bx = (p3.x() - p2.x()) * width, by = (p3.y() - p2.y()) * height;
        double angle = Math.abs(Math.toDegrees(Math.atan2(by, bx) - Math.atan2(ay, ax)));
        if (angle > 180.0) angle = 360.0 - angle;
        return angle;
    }

    public void release() {
        released = true;
        counting = false;
        // đóng trên executor để không đụng lúc đang phân tích dở
        executor.execute(() -> {
            if (poseLandmarker != null) poseLandmarker.close();
        });
        executor.shutdown();
    }
}