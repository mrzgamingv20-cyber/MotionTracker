package com.aku.motiontracker;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
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

    // ExecutorService = thread khusus buat proses kamera di background
    // Supaya tidak lag di main thread (UI thread)
    private ExecutorService cameraExecutor;

    // ObjectDetector = AI dari ML Kit buat deteksi objek
    private ObjectDetector objectDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        tvStatus = findViewById(R.id.tvStatus);
        btnSwitch = findViewById(R.id.btnSwitch);
        overlayView = findViewById(R.id.overlayView);

        // Inisialisasi thread background
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Setup ML Kit Object Detector
        // STREAM_MODE = mode real-time (cocok buat kamera live)
        // enableMultipleObjects = bisa deteksi lebih dari 1 objek sekaligus
        // enableClassification = aktifkan label nama objek
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build();

        // Buat instance detector dari options tadi
        objectDetector = ObjectDetection.getClient(options);

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

                // Preview = tampilan live kamera di layar
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // ImageAnalysis = ambil frame kamera untuk diproses AI
                // STRATEGY_KEEP_ONLY_LATEST = kalau AI masih proses frame lama,
                // frame baru langsung ganti (tidak numpuk di antrian)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

                // Set analyzer = fungsi yang dipanggil setiap ada frame baru
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector selector = isBackCamera ?
                    CameraSelector.DEFAULT_BACK_CAMERA :
                    CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                // Bind preview + imageAnalysis sekaligus ke lifecycle
                cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);
                tvStatus.setText(isBackCamera ? "Kamera Belakang" : "Kamera Depan");

            } catch (Exception e) {
                tvStatus.setText("Error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Method ini dipanggil setiap frame dari kamera
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // Konversi frame kamera ke format yang bisa diproses ML Kit
        InputImage image = InputImage.fromMediaImage(
            imageProxy.getImage(),
            imageProxy.getImageInfo().getRotationDegrees()
        );

        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();

        // Proses frame dengan AI detector
        objectDetector.process(image)
            .addOnSuccessListener(detectedObjects -> {
                // Kalau berhasil, update overlay dengan hasil deteksi
                // runOnUiThread karena overlay harus diupdate di UI thread
                runOnUiThread(() -> {
                    overlayView.updateDetections(detectedObjects, imageWidth, imageHeight);
                    tvStatus.setText("Deteksi: " + detectedObjects.size() + " objek");
                });
            })
            .addOnFailureListener(e -> {
                runOnUiThread(() -> tvStatus.setText("Error deteksi: " + e.getMessage()));
            })
            // PENTING: selalu close imageProxy setelah selesai
            // Kalau tidak di-close, kamera akan berhenti mengirim frame baru
            .addOnCompleteListener(task -> imageProxy.close());
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
        // Matikan thread background dan detector saat app ditutup
        cameraExecutor.shutdown();
        objectDetector.close();
    }
}
