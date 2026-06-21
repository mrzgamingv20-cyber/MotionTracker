package com.aku.motiontracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class DetectionOverlay extends View {

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint = new Paint();

    private List<ObjectDetectorHelper.Detection> detections = new ArrayList<>();
    private int imageWidth = 1;
    private int imageHeight = 1;

    public DetectionOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);

        boxPaint.setColor(Color.GREEN);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setStyle(Paint.Style.FILL);

        bgPaint.setColor(Color.argb(150, 0, 0, 0));
        bgPaint.setStyle(Paint.Style.FILL);
    }

    public void updateDetections(List<ObjectDetectorHelper.Detection> objects,
            int imgWidth, int imgHeight) {
        this.detections = objects;
        this.imageWidth = imgWidth;
        this.imageHeight = imgHeight;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float scaleX = (float) getWidth() / imageWidth;
        float scaleY = (float) getHeight() / imageHeight;

        for (ObjectDetectorHelper.Detection det : detections) {
            RectF box = new RectF(
                det.boundingBox.left * getWidth(),
                det.boundingBox.top * getHeight(),
                det.boundingBox.right * getWidth(),
                det.boundingBox.bottom * getHeight()
            );

            // Gambar kotak hijau
            canvas.drawRect(box, boxPaint);

            // Label nama objek + confidence
            String label = det.label + " " + (int)(det.confidence * 100) + "%";

            // Background label biar mudah dibaca
            float textWidth = textPaint.measureText(label);
            canvas.drawRect(box.left, box.top - 50,
                box.left + textWidth + 10, box.top, bgPaint);

            // Tulis label
            canvas.drawText(label, box.left + 5, box.top - 10, textPaint);
        }
    }
}
