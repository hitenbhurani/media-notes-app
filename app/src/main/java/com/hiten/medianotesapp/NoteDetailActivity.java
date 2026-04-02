package com.hiten.medianotesapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.hiten.medianotesapp.database.NoteRepository;
import com.hiten.medianotesapp.model.Note;

public class NoteDetailActivity extends AppCompatActivity {

    private ImageView ivImage;
    private TextView tvTitle, tvDesc, tvDate, tvType;
    private MaterialButton btnEdit, btnDelete, btnShare;

    private FirebaseAuth auth;
    private NoteRepository noteRepository;

    private String noteId;
    private Note currentNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        auth = FirebaseAuth.getInstance();
        noteRepository = NoteRepository.getInstance(this);

        initUI();
        getNoteIdFromIntent();
        setupActions();
    }

    private void initUI() {
        ivImage = findViewById(R.id.ivDetailImage);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvDesc = findViewById(R.id.tvDetailDescription);
        tvDate = findViewById(R.id.tvDetailDate);
        tvType = findViewById(R.id.tvDetailType);

        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnShare = findViewById(R.id.btnShare);

        Toolbar toolbar = findViewById(R.id.toolbarDetail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void getNoteIdFromIntent() {
        noteId = getIntent().getStringExtra("id");
        if (noteId == null) {
            Toast.makeText(this, "Error: Note ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadNote();
    }

    private void loadNote() {
        if (auth.getCurrentUser() == null || noteId == null) {
            finish();
            return;
        }

        noteRepository.getNoteById(noteId, auth.getCurrentUser().getUid(), new NoteRepository.DataCallback<Note>() {
            @Override
            public void onSuccess(Note data) {
                if (data == null) {
                    finish();
                    return;
                }
                currentNote = data;
                updateUI();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(NoteDetailActivity.this, "Failed to load note", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        tvTitle.setText(TextUtils.isEmpty(currentNote.getTitle()) ? "Untitled note" : currentNote.getTitle());
        tvDesc.setText(TextUtils.isEmpty(currentNote.getDescription()) ? "No description" : currentNote.getDescription());
        tvType.setText(TextUtils.isEmpty(currentNote.getNoteType()) ? "General" : currentNote.getNoteType());
        tvDate.setText(currentNote.getDateFormatted());

        if (currentNote.getImagePath() != null && !currentNote.getImagePath().isEmpty()) {
            Glide.with(this).load(currentNote.getImagePath()).into(ivImage);
        } else {
            ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private void setupActions() {
        btnDelete.setOnClickListener(v -> confirmDelete());

        btnShare.setOnClickListener(v -> {
            if (currentNote == null) return;
            String text = "Title: " + currentNote.getTitle() + "\n\n" + currentNote.getDescription();
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(i, "Share Note"));
        });

        btnEdit.setOnClickListener(v -> {
            if (currentNote == null) return;
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("id", currentNote.getId());
            i.putExtra("title", currentNote.getTitle());
            i.putExtra("description", currentNote.getDescription());
            i.putExtra("image_url", currentNote.getImagePath());
            i.putExtra("note_type", currentNote.getNoteType());
            i.putExtra("is_favorite", currentNote.getIsFavorite());
            i.putExtra("is_done", currentNote.getIsDone());
            i.putExtra("timestamp", currentNote.getTimestamp() != null ? currentNote.getTimestamp().getTime() : -1L);
            i.putExtra("is_edit", true);
            startActivity(i);
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete", (d, w) -> deleteNote())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote() {
        if (currentNote == null) return;

        btnDelete.setEnabled(false);

        noteRepository.deleteNote(currentNote, new NoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(NoteDetailActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception e) {
                btnDelete.setEnabled(true);
                Toast.makeText(NoteDetailActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
