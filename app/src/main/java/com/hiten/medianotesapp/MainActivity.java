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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
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
    private SwitchMaterial switchFavorite;
    private ImageView ivPreview;
    private Button btnCapture, btnSelect, btnSave, btnViewAll;
    private View rootLayout;

    private String currentImagePath = "";
    private boolean isCameraRequest = false;
    private DBHelper dbHelper;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private static final float SHAKE_THRESHOLD = 12.0f;

    // Edit Mode Variables
    private boolean isEditMode = false;
    private int noteIdToEdit = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        rootLayout = findViewById(android.R.id.content);
        initUI();
        setupWorkManager();
        setupSensor();
        checkEditMode();

        btnCapture.setOnClickListener(v -> {
            isCameraRequest = true;
            checkPermissionAndOpenSource();
        });
        btnSelect.setOnClickListener(v -> {
            isCameraRequest = false;
            checkPermissionAndOpenSource();
        });
        btnSave.setOnClickListener(v -> saveOrUpdateNote());
        btnViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NotesActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void initUI() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etNoteType = findViewById(R.id.etNoteType);
        switchFavorite = findViewById(R.id.switchFavorite);
        ivPreview = findViewById(R.id.ivPreview);
        btnCapture = findViewById(R.id.btnCapture);
        btnSelect = findViewById(R.id.btnSelect);
        btnSave = findViewById(R.id.btnSave);
        btnViewAll = findViewById(R.id.btnViewAll);
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("is_edit", false)) {
            isEditMode = true;
            noteIdToEdit = intent.getIntExtra("id", -1);
            etTitle.setText(intent.getStringExtra("title"));
            etDescription.setText(intent.getStringExtra("description"));
            etNoteType.setText(intent.getStringExtra("note_type"));
            currentImagePath = intent.getStringExtra("image_path");
            
            // Set favorite switch state
            int favStatus = intent.getIntExtra("is_favorite", 0);
            switchFavorite.setChecked(favStatus == 1);
            
            if (currentImagePath != null && !currentImagePath.isEmpty()) {
                displayImage(currentImagePath);
            }
            
            btnSave.setText("Update Note");
        }
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
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
        
        if (intent.resolveActivity(getPackageManager()) != null || !list.isEmpty()) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                showSnackbar("File creation failed");
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_CAMERA);
            }
        } else {
            showSnackbar("No camera app found");
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
                        showSnackbar("Failed to load image");
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

    private void saveOrUpdateNote() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String noteType = etNoteType.getText().toString().trim();
        int isFavorite = switchFavorite.isChecked() ? 1 : 0;
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        if (title.isEmpty()) {
            showSnackbar("Title cannot be empty");
            return;
        }

        if (isEditMode) {
            dbHelper.updateNote(noteIdToEdit, title, description, currentImagePath, noteType, isFavorite);
            showSnackbar("Note updated successfully");
            
            Intent intent = new Intent(this, NotesActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            Note note = new Note(0, title, description, currentImagePath, date, noteType);
            note.setIsFavorite(isFavorite);
            long id = dbHelper.insertNote(note);

            if (id > 0) {
                showSnackbar("Note saved successfully");
                clearForm();
                Intent intent = new Intent(this, NotesActivity.class);
                startActivity(intent);
                finish();
            } else {
                showSnackbar("Failed to save note");
            }
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_SHORT).show();
    }

    private void clearForm() {
        etTitle.setText("");
        etDescription.setText("");
        etNoteType.setText("");
        switchFavorite.setChecked(false);
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
                showSnackbar("Device motion detected");
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
                showSnackbar("Permissions denied");
            }
        }
    }
}
