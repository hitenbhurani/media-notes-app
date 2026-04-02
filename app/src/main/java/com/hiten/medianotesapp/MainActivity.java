package com.hiten.medianotesapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.hiten.medianotesapp.database.NoteRepository;
import com.hiten.medianotesapp.model.Note;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etOtherCategory;
    private TextInputLayout tilOtherCategory;
    private ChipGroup chipGroupCategory;
    private SwitchMaterial switchFavorite;
    private ImageView ivPreview;
    private Button btnCapture, btnSelect, btnSave, btnBack;
    private ProgressBar progressBar;
    private View root;

    private FirebaseAuth auth;
    private NoteRepository noteRepository;

    private Uri currentImageUri = null;
    private String photoPath = "";
    private boolean isEditMode = false;
    private String noteIdToEdit = "";
    private int existingDoneStatus = 0;
    private long existingTimestampMillis = -1L;

    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_GALLERY = 102;
    private static final int REQUEST_PERMISSION = 103;

    private boolean pendingCameraAction = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        noteRepository = NoteRepository.getInstance(this);

        root = findViewById(android.R.id.content);

        initUI();
        setupCategoryLogic();
        checkEditMode();

        btnCapture.setOnClickListener(v -> {
            pendingCameraAction = true;
            checkPermissionAndOpenSource();
        });
        btnSelect.setOnClickListener(v -> {
            pendingCameraAction = false;
            checkPermissionAndOpenSource();
        });
        btnSave.setOnClickListener(v -> validateAndSave());
        btnBack.setOnClickListener(v -> {
            if (isEditMode) {
                finish();
            } else {
                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private void checkPermissionAndOpenSource() {
        String[] permissions = getRequiredPermissions(pendingCameraAction);
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            if (pendingCameraAction) {
                openCamera();
            } else {
                openGallery();
            }
            return;
        }

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION);
    }

    private String[] getRequiredPermissions(boolean forCamera) {
        if (forCamera) {
            return new String[]{Manifest.permission.CAMERA};
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        }
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    private void initUI() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etOtherCategory = findViewById(R.id.etNoteType);
        tilOtherCategory = findViewById(R.id.tilOtherCategory);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        switchFavorite = findViewById(R.id.switchFavorite);
        ivPreview = findViewById(R.id.ivPreview);
        btnCapture = findViewById(R.id.btnCapture);
        btnSelect = findViewById(R.id.btnSelect);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnViewAll);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupCategoryLogic() {
        chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipOthers) {
                tilOtherCategory.setVisibility(View.VISIBLE);
            } else {
                tilOtherCategory.setVisibility(View.GONE);
                etOtherCategory.setText("");
            }
        });
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("is_edit", false)) {
            isEditMode = true;
            noteIdToEdit = intent.getStringExtra("id");
            existingDoneStatus = intent.getIntExtra("is_done", 0);
            existingTimestampMillis = intent.getLongExtra("timestamp", -1L);

            etTitle.setText(intent.getStringExtra("title"));
            etDescription.setText(intent.getStringExtra("description"));

            String category = intent.getStringExtra("note_type");
            setCategoryUI(category);

            String imagePath = intent.getStringExtra("image_url");
            if (!TextUtils.isEmpty(imagePath)) {
                currentImageUri = toSafeUri(imagePath);
                Glide.with(this).load(currentImageUri).into(ivPreview);
            }

            switchFavorite.setChecked(intent.getIntExtra("is_favorite", 0) == 1);
            btnSave.setText("Update Note");
            btnBack.setText("Cancel");
        } else {
            btnBack.setText("View Notes");
        }
    }

    private void setCategoryUI(String category) {
        if (TextUtils.isEmpty(category)) return;

        if (category.equals("Work")) chipGroupCategory.check(R.id.chipWork);
        else if (category.equals("Study")) chipGroupCategory.check(R.id.chipStudy);
        else if (category.equals("Personal")) chipGroupCategory.check(R.id.chipPersonal);
        else {
            chipGroupCategory.check(R.id.chipOthers);
            tilOtherCategory.setVisibility(View.VISIBLE);
            if (category.startsWith("Others:")) {
                etOtherCategory.setText(category.substring("Others:".length()));
            } else {
                etOtherCategory.setText(category);
            }
        }
    }

    private String getSelectedCategory() {
        int checkedId = chipGroupCategory.getCheckedChipId();
        if (checkedId == R.id.chipWork) return "Work";
        if (checkedId == R.id.chipStudy) return "Study";
        if (checkedId == R.id.chipPersonal) return "Personal";
        if (checkedId == R.id.chipOthers) {
            String custom = etOtherCategory.getText().toString().trim();
            return TextUtils.isEmpty(custom) ? "Others" : "Others:" + custom;
        }
        return "General";
    }

    private void validateAndSave() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String category = getSelectedCategory();

        if (TextUtils.isEmpty(title)) {
            showSnackbar("Title is required");
            return;
        }

        setLoading(true);

        // Always use a thread for save to avoid UI lag
        new Thread(() -> {
            try {
                String imagePathToSave = "";
                if (currentImageUri != null) {
                    imagePathToSave = resolveLocalImagePath(currentImageUri);
                }
                
                final String finalPath = imagePathToSave;
                runOnUiThread(() -> saveNoteLocal(title, desc, category, finalPath));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showSnackbar("Error processing image: " + e.getMessage());
                });
            }
        }).start();
    }

    private String resolveLocalImagePath(Uri uri) throws IOException {
        if (uri == null) return "";
        
        String uriString = uri.toString();
        // If it's already a local file path that we captured or saved, return it
        if (uriString.startsWith("/") && new File(uriString).exists()) return uriString;
        if (uri.getScheme() != null && uri.getScheme().equals("file")) return uri.getPath();

        // If it's a content URI (Gallery or FileProvider), copy it to our private storage to ensure persistence
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) throw new IOException("Storage unavailable");
        
        String fileName = "NOTE_" + System.currentTimeMillis() + ".jpg";
        File destFile = new File(storageDir, fileName);

        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(destFile)) {
            if (in == null) throw new IOException("Failed to open input stream");
            byte[] buffer = new byte[1024 * 4];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
        return destFile.getAbsolutePath();
    }

    private Uri toSafeUri(String pathOrUri) {
        if (TextUtils.isEmpty(pathOrUri)) {
            return null;
        }
        if (pathOrUri.startsWith("/")) {
            return Uri.fromFile(new File(pathOrUri));
        }
        return Uri.parse(pathOrUri);
    }

    private void saveNoteLocal(String title, String desc, String category, String imagePath) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "guest";
        Note note = new Note(title, desc, imagePath, category, userId);
        note.setIsFavorite(switchFavorite.isChecked() ? 1 : 0);

        if (isEditMode) {
            note.setId(noteIdToEdit);
            note.setIsDone(existingDoneStatus);
            note.setTimestamp(existingTimestampMillis > 0L ? new Date(existingTimestampMillis) : new Date());
            noteRepository.updateNote(note, new NoteRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    setLoading(false);
                    showSnackbar("Update failed: " + e.getMessage());
                }
            });
        } else {
            note.setTimestamp(new Date());
            noteRepository.insertNote(note, new NoteRepository.DataCallback<Long>() {
                @Override
                public void onSuccess(Long data) {
                    setLoading(false);
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    setLoading(false);
                    showSnackbar("Save failed: " + e.getMessage());
                }
            });
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (intent.resolveActivity(getPackageManager()) == null && list.isEmpty()) {
            showSnackbar("No camera app found");
            return;
        }

        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            showSnackbar("Error creating image file");
        }
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            for (ResolveInfo info : list) {
                grantUriPermission(info.activityInfo.packageName, photoURI,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            throw new IOException("Storage unavailable");
        }
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("Failed to create storage directory");
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        photoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY && data != null) {
                currentImageUri = data.getData();
                if (currentImageUri != null) {
                    Glide.with(this).load(currentImageUri).into(ivPreview);
                } else {
                    showSnackbar("Failed to read selected image");
                }
            } else if (requestCode == REQUEST_CAMERA) {
                File file = new File(photoPath);
                if (file.exists()) {
                    currentImageUri = Uri.fromFile(file);
                    Glide.with(this).load(currentImageUri).into(ivPreview);
                } else {
                    showSnackbar("Captured image not found");
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSION) {
            return;
        }

        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            showSnackbar("Required permissions denied");
            return;
        }

        if (pendingCameraAction) {
            openCamera();
        } else {
            openGallery();
        }
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnCapture.setEnabled(!loading);
        btnSelect.setEnabled(!loading);
    }

    private void showSnackbar(String message) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
    }
}
