package com.aku.motiontracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class ObjectDetectorHelper {

    // Label nama objek yang bisa dideteksi (90 kategori COCO dataset)
    private static final String[] LABELS = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train",
        "truck", "boat", "traffic light", "fire hydrant", "stop sign",
        "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep",
        "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
        "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard",
        "sports ball", "kite", "baseball bat", "baseball glove", "skateboard",
        "surfboard", "tennis racket", "bottle", "wine glass", "cup", "fork",
        "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
        "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
        "couch", "potted plant", "bed", "dining table", "toilet", "tv",
        "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
        "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
        "scissors", "teddy bear", "hair drier", "toothbrush"
    };

    // Ukuran input model MobileNet SSD
    private static final int INPUT_SIZE = 300;
    // Maksimal objek yang bisa dideteksi sekaligus
    private static final int MAX_DETECTIONS = 10;

    private Interpreter interpreter;
    private final Context context;

    public static class Detection {
        public RectF boundingBox;
        public String label;
        public float confidence;

        public Detection(RectF box, String label, float confidence) {
            this.boundingBox = box;
            this.label = label;
            this.confidence = confidence;
        }
    }

    public ObjectDetectorHelper(Context context) {
        this.context = context;
    }

    // Load model dari folder assets
    public void initialize() throws IOException {
        MappedByteBuffer model = loadModelFile();
        interpreter = new Interpreter(model);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        // Buka file model dari assets
        android.content.res.AssetFileDescriptor fd =
            context.getAssets().openFd("mobilenet.tflite");
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY,
            fd.getStartOffset(), fd.getDeclaredLength());
    }

    // Konversi bitmap ke ByteBuffer yang bisa dibaca model
    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        // Model MobileNet butuh input 300x300x3 (RGB) dalam format uint8
        ByteBuffer buffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixel : pixels) {
            // Ekstrak channel R, G, B dari pixel
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));          // B
        }
        return buffer;
    }

    public List<Detection> detect(Bitmap bitmap) {
        List<Detection> results = new ArrayList<>();
        if (interpreter == null) return results;

        // Resize bitmap ke 300x300 (ukuran input model)
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        ByteBuffer input = bitmapToByteBuffer(resized);

        // Output model MobileNet SSD:
        // [0] = koordinat bounding box [1,10,4]
        // [1] = index label [1,10]
        // [2] = confidence score [1,10]
        // [3] = jumlah deteksi [1]
        float[][][] boxes = new float[1][MAX_DETECTIONS][4];
        float[][] classes = new float[1][MAX_DETECTIONS];
        float[][] scores = new float[1][MAX_DETECTIONS];
        float[] numDetections = new float[1];

        Object[] inputs = {input};
        java.util.Map<Integer, Object> outputs = new java.util.HashMap<>();
        outputs.put(0, boxes);
        outputs.put(1, classes);
        outputs.put(2, scores);
        outputs.put(3, numDetections);

        // Jalanin model
        interpreter.runForMultipleInputsOutputs(inputs, outputs);

        // Parse hasil deteksi
        int count = (int) numDetections[0];
        for (int i = 0; i < count; i++) {
            float confidence = scores[0][i];

            // Cuma tampilkan kalau confidence > 50%
            if (confidence > 0.5f) {
                // Koordinat box dalam format [top, left, bottom, right]
                float top = boxes[0][i][0];
                float left = boxes[0][i][1];
                float bottom = boxes[0][i][2];
                float right = boxes[0][i][3];

                R
cat > ~/MotionTracker/app/src/main/java/com/aku/motiontracker/ObjectDetectorHelper.java << 'EOF'
package com.aku.motiontracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectDetectorHelper {

    private static final String[] LABELS = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train",
        "truck", "boat", "traffic light", "fire hydrant", "stop sign",
        "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep",
        "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
        "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard",
        "sports ball", "kite", "baseball bat", "baseball glove", "skateboard",
        "surfboard", "tennis racket", "bottle", "wine glass", "cup", "fork",
        "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
        "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
        "couch", "potted plant", "bed", "dining table", "toilet", "tv",
        "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
        "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase",
        "scissors", "teddy bear", "hair drier", "toothbrush"
    };

    private static final int INPUT_SIZE = 300;
    private static final int MAX_DETECTIONS = 10;
    private Interpreter interpreter;
    private final Context context;

    public static class Detection {
        public RectF boundingBox;
        public String label;
        public float confidence;

        public Detection(RectF box, String label, float confidence) {
            this.boundingBox = box;
            this.label = label;
            this.confidence = confidence;
        }
    }

    public ObjectDetectorHelper(Context context) {
        this.context = context;
    }

    public void initialize() throws IOException {
        MappedByteBuffer model = loadModelFile();
        interpreter = new Interpreter(model);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        android.content.res.AssetFileDescriptor fd =
            context.getAssets().openFd("mobilenet.tflite");
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY,
            fd.getStartOffset(), fd.getDeclaredLength());
    }

    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
        }
        return buffer;
    }

    public List<Detection> detect(Bitmap bitmap) {
        List<Detection> results = new ArrayList<>();
        if (interpreter == null) return results;

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        ByteBuffer input = bitmapToByteBuffer(resized);

        float[][][] boxes = new float[1][MAX_DETECTIONS][4];
        float[][] classes = new float[1][MAX_DETECTIONS];
        float[][] scores = new float[1][MAX_DETECTIONS];
        float[] numDetections = new float[1];

        Object[] inputs = {input};
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, boxes);
        outputs.put(1, classes);
        outputs.put(2, scores);
        outputs.put(3, numDetections);

        interpreter.runForMultipleInputsOutputs(inputs, outputs);

        int count = (int) numDetections[0];
        for (int i = 0; i < count; i++) {
            float confidence = scores[0][i];
            if (confidence > 0.5f) {
                float top = boxes[0][i][0];
                float left = boxes[0][i][1];
                float bottom = boxes[0][i][2];
                float right = boxes[0][i][3];
                RectF box = new RectF(left, top, right, bottom);
                int labelIndex = (int) classes[0][i];
                String label = labelIndex < LABELS.length ? LABELS[labelIndex] : "unknown";
                results.add(new Detection(box, label, confidence));
            }
        }
        return results;
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }
}
