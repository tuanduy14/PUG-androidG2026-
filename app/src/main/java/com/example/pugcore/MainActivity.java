package com.example.pugcore;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker.PoseLandmarkerOptions;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PushUpRPG";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView viewFinder;
    private TextView tvPushUpCount;

    private PoseLandmarker poseLandmarker;
    private ExecutorService backgroundExecutor;


    private int pushUpCount = 0; // Biến đếm số lần chống đẩy
    private boolean isDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFinder = findViewById(R.id.viewFinder);
        tvPushUpCount = findViewById(R.id.tvPushUpCount);
        backgroundExecutor = Executors.newSingleThreadExecutor();

        setupPoseLandmarker();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    // Khởi tạo MediaPipe Pose Landmarker ở chế độ LIVE_STREAM
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

            poseLandmarker = PoseLandmarker.createFromOptions(this, options);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khởi tạo MediaPipe: " + e.getMessage(), e);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // Cấu hình ImageAnalysis để gửi từng khung hình cho MediaPipe
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();

                imageAnalysis.setAnalyzer(backgroundExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                try {
                    if (!cameraProvider.hasCamera(cameraSelector)) {
                        if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                        } else if (!cameraProvider.getAvailableCameraInfos().isEmpty()) {
                            cameraSelector = cameraProvider.getAvailableCameraInfos().get(0).getCameraSelector();
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Lỗi kiểm tra camera: " + e.getMessage());
                }

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khởi chạy Camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Chuyển đổi khung hình từ CameraX sang định dạng MPImage của MediaPipe
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (poseLandmarker == null) {
            imageProxy.close();
            return;
        }

        Bitmap bitmap = imageProxy.toBitmap();
        Matrix matrix = new Matrix();
        matrix.postRotate(imageProxy.getImageInfo().getRotationDegrees());

        Bitmap rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        MPImage mpImage = new BitmapImageBuilder(rotatedBitmap).build();
        long frameTime = SystemClock.uptimeMillis();

        poseLandmarker.detectAsync(mpImage, frameTime);
        imageProxy.close();
    }

    // Xử lý kết quả trả về từ AI MediaPipe
    private void processPoseResult(PoseLandmarkerResult result, MPImage inputImage) {
        if (result.landmarks().isEmpty()) return;

        List<NormalizedLandmark> landmarks = result.landmarks().get(0);

        // Lấy tọa độ các khớp xương cánh tay trái
        // 11: Vai trái (Left Shoulder), 13: Khuỷu tay trái (Left Elbow), 15: Cổ tay trái (Left Wrist)
        NormalizedLandmark shoulder = landmarks.get(11);
        NormalizedLandmark elbow = landmarks.get(13);
        NormalizedLandmark wrist = landmarks.get(15);

        double angle = calculateAngle(shoulder, elbow, wrist);

        // Thuật toán đếm số lần chống đẩy dựa vào góc khuỷu tay
        // Góc < 90 độ: Đang co tay (hạ người xuống - Down)
        // Góc > 160 độ: Đã duỗi thẳng tay (đẩy người lên - Up) -> Tính 1 Rep
        if (angle < 90) {
            isDown = true;
        } else if (angle > 160 && isDown) {
            pushUpCount++;
            isDown = false;
            runOnUiThread(() -> tvPushUpCount.setText("Push-up: " + pushUpCount));
        }
    }

    // Hàm tính góc tạo bởi 3 điểm (Vai - Khuỷu tay - Cổ tay)
    private double calculateAngle(NormalizedLandmark p1, NormalizedLandmark p2, NormalizedLandmark p3) {
        double radians = Math.atan2(p3.y() - p2.y(), p3.x() - p2.x()) -
                Math.atan2(p1.y() - p2.y(), p1.x() - p2.x());
        double angle = Math.abs(Math.toDegrees(radians));
        if (angle > 180.0) {
            angle = 360.0 - angle;
        }
        return angle;
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundExecutor != null) {
            backgroundExecutor.shutdown();
        }
        if (poseLandmarker != null) {
            poseLandmarker.close();
        }
    }
}