package com.aku.motiontracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.google.mlkit.vision.objects.DetectedObject;
import java.util.ArrayList;
import java.util.List;

// View khusus buat gambar kotak di atas objek yang terdeteksi
public class DetectionOverlay extends View {

    // Paint = alat gambar, seperti kuas di Canvas
    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();

    // List objek yang terdeteksi dari kamera
    private List<DetectedObject> detectedObjects = new ArrayList<>();

    // Ukuran preview kamera (buat scale koordinat)
    private int imageWidth = 1;
    private int imageHeight = 1;

    public DetectionOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Setup garis kotak hijau
        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);

        // Setup teks label putih
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setStyle(Paint.Style.FILL);
    }

    // Method ini dipanggil setiap kali ada deteksi baru dari kamera
    public void updateDetections(List<DetectedObject> objects, int imgWidth, int imgHeight) {
        this.detectedObjects = objects;
        this.imageWidth = imgWidth;
        this.imageHeight = imgHeight;
        // invalidate() = paksa View ini untuk digambar ulang
        invalidate();
    }

    // onDraw dipanggil otomatis setiap kali invalidate() dipanggil
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Hitung ratio scale antara ukuran kamera vs ukuran layar
        float scaleX = (float) getWidth() / imageWidth;
        float scaleY = (float) getHeight() / imageHeight;

        for (DetectedObject obj : detectedObjects) {
            // Ambil bounding box dari objek
            RectF box = new RectF(obj.getBoundingBox());

            // Scale koordinat dari ukuran kamera ke ukuran layar
            box.left *= scaleX;
            box.right *= scaleX;
            box.top *= scaleY;
            box.bottom *= scaleY;

            // Gambar kotak hijau di sekitar objek
            canvas.drawRect(box, boxPaint);

            // Ambil label objek (kalau ada)
            String label = "Objek";
            if (!obj.getLabels().isEmpty()) {
                label = obj.getLabels().get(0).getText();
                float confidence = obj.getLabels().get(0).getConfidence();
                label = label + " " + (int)(confidence * 100) + "%";
            }

            // Tulis label di atas kotak
            canvas.drawText(label, box.left, box.top - 10, textPaint);
        }
    }
}
