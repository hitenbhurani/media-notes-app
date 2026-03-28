package com.hiten.medianotesapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.hiten.medianotesapp.database.DBHelper;
import com.hiten.medianotesapp.model.Note;
import com.hiten.medianotesapp.worker.NotesWorker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "CAMERA_DEBUG";
    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_GALLERY = 102;
    private static final int PERMISSION_CODE = 100;

    private EditText etTitle, etDescription, etNoteType;
    private ImageView ivPreview;
    private Button btnCapture, btnSelect, btnSave, btnViewAll;

    private String currentImagePath = "";
    private boolean isCameraRequest = false;
    private DBHelper dbHelper;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private static final float SHAKE_THRESHOLD = 12.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        initUI();
        setupWorkManager();
        setupSensor();

        btnCapture.setOnClickListener(v -> {
            isCameraRequest = true;
            checkPermissionAndOpenSource();
        });
        btnSelect.setOnClickListener(v -> {
            isCameraRequest = false;
            checkPermissionAndOpenSource();
        });
        btnSave.setOnClickListener(v -> saveNote());
        btnViewAll.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NotesActivity.class)));
    }

    private void initUI() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etNoteType = findViewById(R.id.etNoteType);
        ivPreview = findViewById(R.id.ivPreview);
        btnCapture = findViewById(R.id.btnCapture);
        btnSelect = findViewById(R.id.btnSelect);
        btnSave = findViewById(R.id.btnSave);
        btnViewAll = findViewById(R.id.btnViewAll);
    }

    private void setupWorkManager() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(NotesWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueue(workRequest);
    }

    private void setupSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void checkPermissionAndOpenSource() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_CODE);
        } else {
            if (isCameraRequest) openCamera(); else openGallery();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Diagnostic Log
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
        Log.d(TAG, "Camera apps found: " + list.size());

        if (intent.resolveActivity(getPackageManager()) != null || !list.isEmpty()) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "File creation failed", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        photoFile
                );

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // 🔥 CRITICAL FIX: Grant permissions to the intent
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivityForResult(intent, REQUEST_CAMERA);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "StartActivity failed: " + e.getMessage());
                    Toast.makeText(this, "Camera failed to open", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.e(TAG, "No camera app found by resolveActivity");
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentImagePath = image.getAbsolutePath();
        Log.d("IMAGE_PATH", "File path: " + currentImagePath);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                displayImage(currentImagePath);
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    try {
                        currentImagePath = saveGalleryImage(selectedImage);
                        displayImage(currentImagePath);
                    } catch (IOException e) {
                        Log.e(TAG, "Gallery save failed: " + e.getMessage());
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void displayImage(String path) {
        if (path != null && !path.isEmpty()) {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ivPreview.setImageBitmap(myBitmap);
            }
        }
    }

    private String saveGalleryImage(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File file = createImageFile();
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while (inputStream != null && (bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.close();
        if (inputStream != null) inputStream.close();
        return file.getAbsolutePath();
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String noteType = etNoteType.getText().toString().trim();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        if (title.isEmpty()) {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Note note = new Note(0, title, description, currentImagePath, date, noteType);
        long id = dbHelper.insertNote(note);

        if (id > 0) {
            Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show();
            clearForm();
        } else {
            Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        etTitle.setText("");
        etDescription.setText("");
        etNoteType.setText("");
        ivPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        currentImagePath = "";
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float deltaX = Math.abs(lastX - x);
            float deltaY = Math.abs(lastY - y);
            float deltaZ = Math.abs(lastZ - z);

            if ((deltaX > SHAKE_THRESHOLD && deltaY > SHAKE_THRESHOLD) ||
                (deltaX > SHAKE_THRESHOLD && deltaZ > SHAKE_THRESHOLD) ||
                (deltaY > SHAKE_THRESHOLD && deltaZ > SHAKE_THRESHOLD)) {
                Toast.makeText(this, "Device motion detected", Toast.LENGTH_SHORT).show();
            }

            lastX = x; lastY = y; lastZ = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                if (isCameraRequest) openCamera(); else openGallery();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
