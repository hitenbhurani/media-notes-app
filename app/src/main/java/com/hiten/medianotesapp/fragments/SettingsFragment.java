package com.hiten.medianotesapp.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.hiten.medianotesapp.LoginActivity;
import com.hiten.medianotesapp.R;
import com.hiten.medianotesapp.database.NoteRepository;
import com.hiten.medianotesapp.model.Note;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private TextView tvEmail;
    private MaterialButton btnLogout, btnClearNotes, btnExportData;
    private SwitchMaterial switchDarkMode;

    private FirebaseAuth auth;
    private NoteRepository noteRepository;
    private GoogleSignInClient mGoogleSignInClient;

    private SharedPreferences prefs;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final String PREFS_NAME = "settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        auth = FirebaseAuth.getInstance();
    noteRepository = NoteRepository.getInstance(requireContext());
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, 0);

        // Configure Google Sign In to handle logout correctly
        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        tvEmail = view.findViewById(R.id.tvUserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnClearNotes = view.findViewById(R.id.btnClearNotes);
        btnExportData = view.findViewById(R.id.btnExportData);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);

        setupUI();
        setupActions();

        return view;
    }

    private void setupUI() {
        if (auth.getCurrentUser() != null) {
            String email = auth.getCurrentUser().getEmail();
            tvEmail.setText(email != null && !email.trim().isEmpty() ? email : "No email");
        } else {
            tvEmail.setText("Not logged in");
        }

        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(darkMode);
    }

    private void setupActions() {

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean current = prefs.getBoolean(KEY_DARK_MODE, false);
            if (current == isChecked) {
                return;
            }

            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();

            switchDarkMode.setEnabled(false);

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            uiHandler.postDelayed(() -> {
                if (!isAdded()) return;
                switchDarkMode.setEnabled(true);
            }, 500);
        });

        btnClearNotes.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Log.w(TAG, "Clear notes blocked: auth user is null");
                showToastSafely("Please login again");
                return;
            }

            btnClearNotes.setEnabled(false);
            String userId = auth.getCurrentUser().getUid();
            Log.d(TAG, "Clear notes requested for userId=" + userId);

            noteRepository.deleteAllNotes(userId, new NoteRepository.DataCallback<Integer>() {
                @Override
                public void onSuccess(Integer deletedCount) {
                    if (!isAdded()) return;
                    Log.d(TAG, "Deleted notes count=" + deletedCount);
                    if (deletedCount == null || deletedCount == 0) {
                        showToastSafely("No notes to delete");
                    } else {
                        showToastSafely("All notes deleted");
                    }
                    btnClearNotes.setEnabled(true);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Local delete failed for userId=" + userId, e);
                    if (isAdded()) {
                        showToastSafely("Failed to clear notes: " + e.getMessage());
                    }
                    btnClearNotes.setEnabled(true);
                }
            });
        });

        btnExportData.setOnClickListener(v -> exportNotesReport());

        btnLogout.setOnClickListener(v -> {
            btnLogout.setEnabled(false);

            // Sign out from Firebase
            auth.signOut();

            // Sign out from Google to allow account selection next time
            if (mGoogleSignInClient != null) {
                mGoogleSignInClient.signOut()
                        .addOnCompleteListener(task -> navigateToLogin())
                        .addOnFailureListener(e -> navigateToLogin());
            } else {
                navigateToLogin();
            }
        });
    }

    private void exportNotesReport() {
        if (auth.getCurrentUser() == null) {
            showToastSafely("Please login again");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        btnExportData.setEnabled(false);

        noteRepository.getAllNotes(userId, new NoteRepository.DataCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> data) {
                if (!isAdded()) return;
                try {
                    File pdf = createNotesPdf(data);
                    sharePdf(pdf);
                    showToastSafely("Report ready");
                } catch (Exception e) {
                    showToastSafely("Export failed: " + e.getMessage());
                } finally {
                    btnExportData.setEnabled(true);
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                btnExportData.setEnabled(true);
                showToastSafely("Failed to load notes for export");
            }
        });
    }

    private File createNotesPdf(List<Note> notes) throws Exception {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        final int pageWidth = 595;
        final int pageHeight = 842;
        final int margin = 40;
        final int rowHeight = 20;

        int pageNum = 1;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        int y = 60;

        paint.setColor(Color.BLACK);
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("MediaNotes Export Report", margin, y, paint);

        y += 24;
        paint.setFakeBoldText(false);
        paint.setTextSize(11f);
        canvas.drawText("Generated: " + new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(new Date()), margin, y, paint);

        y += 28;
        paint.setFakeBoldText(true);
        canvas.drawText("Title", margin, y, paint);
        canvas.drawText("Category", 270, y, paint);
        canvas.drawText("Date", 410, y, paint);
        paint.setFakeBoldText(false);

        y += 8;
        canvas.drawLine(margin, y, pageWidth - margin, y, paint);
        y += 16;

        for (Note note : notes) {
            if (y > pageHeight - 60) {
                document.finishPage(page);
                pageNum++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 60;
            }

            String title = note.getTitle() == null ? "Untitled" : note.getTitle();
            String category = note.getNoteType() == null ? "General" : note.getNoteType();
            String date = note.getDateFormatted();

            if (title.length() > 34) {
                title = title.substring(0, 31) + "...";
            }
            if (category.length() > 16) {
                category = category.substring(0, 13) + "...";
            }

            paint.setTextSize(10f);
            canvas.drawText(title, margin, y, paint);
            canvas.drawText(category, 270, y, paint);
            canvas.drawText(date, 410, y, paint);
            y += rowHeight;
        }

        document.finishPage(page);

        File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir == null) {
            throw new IllegalStateException("Storage unavailable");
        }
        File out = new File(dir, "MediaNotes_Report.pdf");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            document.writeTo(fos);
        } finally {
            document.close();
        }
        return out;
    }

    private void sharePdf(File file) {
        Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share notes report"));
    }

    private void navigateToLogin() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showToastSafely(String msg) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
