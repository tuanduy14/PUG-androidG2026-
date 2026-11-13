package com.example.pugcore;

import android.app.Application;
import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;

/**
 * Custom Application class để cấu hình CameraX toàn cục.
 * Tránh lỗi CameraIdListIncorrectException khi chạy trên emulator hoặc thiết bị
 * thiếu camera trước/sau.
 */
public class PugApplication extends Application implements CameraXConfig.Provider {

    private static final String TAG = "PugApplication";

    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        boolean hasFront = false;
        boolean hasBack = false;

        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager != null) {
                String[] cameraIds = cameraManager.getCameraIdList();
                for (String id : cameraIds) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null) {
                        if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                            hasFront = true;
                        } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                            hasBack = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi kiểm tra danh sách camera: " + e.getMessage(), e);
        }

        CameraXConfig.Builder configBuilder = CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig());

        if (hasFront && hasBack) {
            Log.i(TAG, "Thiết bị có cả camera trước và sau");
        } else if (hasFront) {
            Log.i(TAG, "Thiết bị chỉ có camera trước -> Giới hạn CameraX dùng DEFAULT_FRONT_CAMERA");
            configBuilder.setAvailableCamerasLimiter(CameraSelector.DEFAULT_FRONT_CAMERA);
        } else if (hasBack) {
            Log.i(TAG, "Thiết bị chỉ có camera sau -> Giới hạn CameraX dùng DEFAULT_BACK_CAMERA");
            configBuilder.setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA);
        } else {
            Log.w(TAG, "Không tìm thấy LENS_FACING chuẩn -> Giới hạn theo camera khả dụng bất kỳ");
            configBuilder.setAvailableCamerasLimiter(new CameraSelector.Builder()
                    .addCameraFilter(cameraInfos -> cameraInfos)
                    .build());
        }

        return configBuilder.build();
    }
}
