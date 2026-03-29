package com.hiten.medianotesapp.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
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

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private TextView tvEmail;
    private MaterialButton btnLogout, btnClearNotes;
    private SwitchMaterial switchDarkMode;

    private FirebaseAuth auth;
    private NoteRepository noteRepository;
    private GoogleSignInClient mGoogleSignInClient;

    private SharedPreferences prefs;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isApplyingTheme = false;
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
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("537308465555-vlj2o6nj4123q5celqslakfq2n7ksn5n.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        tvEmail = view.findViewById(R.id.tvUserEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnClearNotes = view.findViewById(R.id.btnClearNotes);
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
            if (isApplyingTheme) return;

            prefs.edit().putBoolean(KEY_DARK_MODE, isChecked).apply();

            int desiredMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            if (AppCompatDelegate.getDefaultNightMode() == desiredMode) {
                return;
            }

            // Prevent rapid taps during recreation-triggering mode change.
            isApplyingTheme = true;
            switchDarkMode.setEnabled(false);

            AppCompatDelegate.setDefaultNightMode(desiredMode);

            uiHandler.postDelayed(() -> {
                if (!isAdded()) return;
                isApplyingTheme = false;
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
