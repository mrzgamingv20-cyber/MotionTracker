package com.aku.motiontracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private TextView tvStatus;
    private Button btnSwitch;
    private DetectionOverlay overlayView;
    private ProcessCameraProvider cameraProvider;
    private boolean isBackCamera = true;
    private static final int REQUEST_CAMERA = 100;
    private ExecutorService cameraExecutor;
    private ObjectDetectorHelper detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        tvStatus = findViewById(R.id.tvStatus);
        btnSwitch = findViewById(R.id.btnSwitch);
        overlayView = findViewById(R.id.overlayView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Inisialisasi detector TFLite
        detector = new ObjectDetectorHelper(this);
        try {
            detector.initialize();
            tvStatus.setText("Model loaded!");
        } catch (Exception e) {
            tvStatus.setText("Error load model: " + e.getMessage());
        }

        btnSwitch.setOnClickListener(v -> {
            isBackCamera = !isBackCamera;
            startCamera();
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
            ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector selector = isBackCamera ?
                    CameraSelector.DEFAULT_BACK_CAMERA :
                    CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);
                tvStatus.setText(isBackCamera ? "Kamera Belakang" : "Kamera Depan");

            } catch (Exception e) {
                tvStatus.setText("Error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(ImageProxy imageProxy) {
        // Konversi ImageProxy ke Bitmap
        Bitmap bitmap = imageProxy.toBitmap();
        int imgWidth = imageProxy.getWidth();
        int imgHeight = imageProxy.getHeight();

        // Jalanin detector
        List<ObjectDetectorHelper.Detection> detections = detector.detect(bitmap);

        // Update UI di main thread
        runOnUiThread(() -> {
            overlayView.updateDetections(detections, imgWidth, imgHeight);
            tvStatus.setText("Deteksi: " + detections.size() + " objek");
        });

        imageProxy.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        detector.close();
    }
}
