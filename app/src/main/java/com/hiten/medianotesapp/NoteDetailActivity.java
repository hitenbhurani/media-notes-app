package com.hiten.medianotesapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.hiten.medianotesapp.database.DBHelper;
import com.hiten.medianotesapp.model.Note;

import java.io.File;

public class NoteDetailActivity extends AppCompatActivity {

    private ImageView ivDetailImage;
    private TextView tvDetailTitle, tvDetailDescription, tvDetailDate, tvDetailType;
    private MaterialButton btnEdit, btnDelete, btnShare;
    private DBHelper dbHelper;

    private int noteId;
    private String title, description, imagePath, date, noteType;
    private int isFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        dbHelper = new DBHelper(this);
        initUI();
        getIntentData();
        setupActions();
    }

    private void initUI() {
        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailDate = findViewById(R.id.tvDetailDate);
        tvDetailType = findViewById(R.id.tvDetailType);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnShare = findViewById(R.id.btnShare);

        Toolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            noteId = intent.getIntExtra("id", -1);
            title = intent.getStringExtra("title");
            description = intent.getStringExtra("description");
            imagePath = intent.getStringExtra("image_path");
            date = intent.getStringExtra("date");
            noteType = intent.getStringExtra("note_type");
            isFavorite = intent.getIntExtra("is_favorite", 0);

            // NULL SAFETY
            if (title == null) title = "";
            if (description == null) description = "";
            if (noteType == null) noteType = "General";
            if (date == null) date = "";

            tvDetailTitle.setText(title);
            tvDetailDescription.setText(description);
            tvDetailDate.setText(date);
            tvDetailType.setText(noteType);

            // IMAGE CRASH FIX
            if (imagePath != null && !imagePath.isEmpty()) {
                File imgFile = new File(imagePath);
                if (imgFile.exists()) {
                    try {
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        if (bitmap != null) {
                            ivDetailImage.setImageBitmap(bitmap);
                        } else {
                            ivDetailImage.setImageResource(android.R.drawable.ic_menu_gallery);
                        }
                    } catch (Exception e) {
                        ivDetailImage.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } else {
                    ivDetailImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } else {
                ivDetailImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }

    private void setupActions() {
        btnDelete.setOnClickListener(v -> showDeleteDialog());
        btnShare.setOnClickListener(v -> shareNote());
        btnEdit.setOnClickListener(v -> {
            // Fix: Open MainActivity in Edit Mode
            Intent intent = new Intent(NoteDetailActivity.this, MainActivity.class);
            intent.putExtra("id", noteId);
            intent.putExtra("title", title);
            intent.putExtra("description", description);
            intent.putExtra("image_path", imagePath);
            intent.putExtra("note_type", noteType);
            intent.putExtra("is_edit", true);
            startActivity(intent);
        });
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteNote(noteId);
                    Toast.makeText(NoteDetailActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void shareNote() {
        String shareBody = "Title: " + title + "\n\n" + description;
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share note via"));
    }
}
