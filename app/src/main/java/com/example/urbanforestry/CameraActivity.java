package com.example.urbanforestry;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraActivity extends AppCompatActivity {

    private final static String TAG = "CAMERA_APP";

    private TextureView textureView;
    private String cameraId;

    protected CameraDevice cameraDevice;
    protected CameraCaptureSession captureSession;
    protected CaptureRequest.Builder previewRequestBuilder;

    private ImageReader imageReader;

    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private static final int REQUEST_CAMERA_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        textureView = findViewById(R.id.camera_preview_tv);
        ImageButton btnCapture = findViewById(R.id.camera_button);

        btnCapture.setOnClickListener(v -> takePicture());

        // Permission check
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);

        } else {
            setupCameraPreview();
        }
    }

    // ========================
    // Permission Result
    // ========================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                setupCameraPreview();

            } else {
                Toast.makeText(this,
                        "Camera permission required",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ========================
    // Setup Preview
    // ========================
    private void setupCameraPreview() {
        textureView.setSurfaceTextureListener(textureListener);
    }

    TextureView.SurfaceTextureListener textureListener =
            new TextureView.SurfaceTextureListener() {

                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface,
                                                      int width, int height) {
                    openCamera();
                }

                @Override public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture s, int w, int h) {}
                @Override public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture s) { return false; }
                @Override public void onSurfaceTextureUpdated(@NonNull SurfaceTexture s) {}
            };

    // ========================
    // Open Camera
    // ========================
    private void openCamera() {

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CameraManager manager =
                (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            cameraId = manager.getCameraIdList()[0]; // back camera
            manager.openCamera(cameraId, stateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {

                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
            };

    // ========================
    // Preview Session
    // ========================
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(1920, 1080);

            Surface surface = new Surface(texture);

            previewRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(
                    Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;

                            previewRequestBuilder.set(
                                    CaptureRequest.CONTROL_MODE,
                                    CameraMetadata.CONTROL_MODE_AUTO
                            );

                            try {
                                captureSession.setRepeatingRequest(
                                        previewRequestBuilder.build(),
                                        null,
                                        backgroundHandler
                                );
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
                    },
                    backgroundHandler
            );

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // ========================
    // Take Picture
    // ========================
    protected void takePicture() {
        if (cameraDevice == null) return;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        try {
            imageReader = ImageReader.newInstance(
                    1920, 1080,
                    ImageFormat.JPEG,
                    1
            );

            imageReader.setOnImageAvailableListener(reader -> {
                try (Image image = reader.acquireLatestImage()) {

                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    save(bytes);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }, backgroundHandler);

            CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(
                            CameraDevice.TEMPLATE_STILL_CAPTURE
                    );

            captureBuilder.addTarget(imageReader.getSurface());

            rotation = getWindowManager().getDefaultDisplay().getRotation();
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            int jpegOrientation = (sensorOrientation + getJpegOrientation(rotation)) % 360;

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);

            cameraDevice.createCaptureSession(
                    Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.capture(
                                        captureBuilder.build(),
                                        null,
                                        backgroundHandler
                                );
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {}
                    },
                    backgroundHandler
            );

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // ========================
    // Save Image
    // ========================
    private void save(byte[] bytes) {
        File file = new File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "pic_" + System.currentTimeMillis() + ".jpg"
        );

        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);

            String path = file.getAbsolutePath();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("imagePath", path);

            setResult(RESULT_OK, resultIntent);
            finish(); // go back to FeedActivity

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================
    // Lifecycle
    // ========================
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();

        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    // ========================
    // Background Thread
    // ========================
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // ========================
    // Cleanup
    // ========================
    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private int getJpegOrientation(int deviceRotation) {
        int rotationDegrees = 0;

        switch (deviceRotation) {
            case Surface.ROTATION_0: rotationDegrees = 0; break;
            case Surface.ROTATION_90: rotationDegrees = 90; break;
            case Surface.ROTATION_180: rotationDegrees = 180; break;
            case Surface.ROTATION_270: rotationDegrees = 270; break;
        }

        return rotationDegrees;
    }
}