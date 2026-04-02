package com.hiten.medianotesapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hiten.medianotesapp.fragments.ActivityFragment;
import com.hiten.medianotesapp.fragments.FavoritesFragment;
import com.hiten.medianotesapp.fragments.HomeFragment;
import com.hiten.medianotesapp.fragments.SettingsFragment;
import com.hiten.medianotesapp.work.ReminderWorkScheduler;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG_HOME = "1";
    private static final String TAG_FAVORITES = "2";
    private static final String TAG_ACTIVITY = "3";
    private static final String TAG_SETTINGS = "4";

    private Fragment homeFragment, favoritesFragment, activityFragment, settingsFragment;
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment active;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        requestNotificationPermissionIfNeeded();
        ReminderWorkScheduler.schedulePeriodicReminder(getApplicationContext());

        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        BottomAppBar bottomAppBar = findViewById(R.id.bottomAppBar);
        BottomNavigationView navView = findViewById(R.id.bottomNavigationView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        applyBottomBarSpacing(fragmentContainer, bottomAppBar);

        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            favoritesFragment = new FavoritesFragment();
            activityFragment = new ActivityFragment();
            settingsFragment = new SettingsFragment();

            fm.beginTransaction()
                    .add(R.id.fragment_container, settingsFragment, TAG_SETTINGS).hide(settingsFragment)
                    .add(R.id.fragment_container, activityFragment, TAG_ACTIVITY).hide(activityFragment)
                    .add(R.id.fragment_container, favoritesFragment, TAG_FAVORITES).hide(favoritesFragment)
                    .add(R.id.fragment_container, homeFragment, TAG_HOME)
                    .commitNow();
            active = homeFragment;
        } else {
            homeFragment = fm.findFragmentByTag(TAG_HOME);
            favoritesFragment = fm.findFragmentByTag(TAG_FAVORITES);
            activityFragment = fm.findFragmentByTag(TAG_ACTIVITY);
            settingsFragment = fm.findFragmentByTag(TAG_SETTINGS);

            if (homeFragment == null) homeFragment = new HomeFragment();
            if (favoritesFragment == null) favoritesFragment = new FavoritesFragment();
            if (activityFragment == null) activityFragment = new ActivityFragment();
            if (settingsFragment == null) settingsFragment = new SettingsFragment();

            if (fm.findFragmentByTag(TAG_HOME) == null) {
                fm.beginTransaction()
                        .add(R.id.fragment_container, settingsFragment, TAG_SETTINGS).hide(settingsFragment)
                        .add(R.id.fragment_container, activityFragment, TAG_ACTIVITY).hide(activityFragment)
                        .add(R.id.fragment_container, favoritesFragment, TAG_FAVORITES).hide(favoritesFragment)
                        .add(R.id.fragment_container, homeFragment, TAG_HOME)
                    .commitNow();
            }
            
            // Determine which was active
            active = homeFragment;
            if (homeFragment != null && homeFragment.isHidden()) {
                if (favoritesFragment != null && !favoritesFragment.isHidden()) active = favoritesFragment;
                else if (activityFragment != null && !activityFragment.isHidden()) active = activityFragment;
                else if (settingsFragment != null && !settingsFragment.isHidden()) active = settingsFragment;
            }
        }

        navView.setSelectedItemId(getMenuIdFor(active));

        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment next = null;
            if (itemId == R.id.nav_home) next = homeFragment;
            else if (itemId == R.id.nav_favorites) next = favoritesFragment;
            else if (itemId == R.id.nav_notifications) next = activityFragment;
            else if (itemId == R.id.nav_settings) next = settingsFragment;

            if (next == null) {
                return false;
            }
            if (next == active) {
                return true;
            }

            if (next != active) {
                fm.beginTransaction().hide(active).show(next).commit();
                active = next;
                return true;
            }
            return true;
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private int getMenuIdFor(Fragment fragment) {
        if (fragment == favoritesFragment) {
            return R.id.nav_favorites;
        }
        if (fragment == activityFragment) {
            return R.id.nav_notifications;
        }
        if (fragment == settingsFragment) {
            return R.id.nav_settings;
        }
        return R.id.nav_home;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void applyBottomBarSpacing(FrameLayout fragmentContainer, BottomAppBar bottomAppBar) {
        if (fragmentContainer == null || bottomAppBar == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomAppBar, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), systemBars.bottom);

            view.post(() -> {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
                int desiredMargin = view.getHeight();
                if (params.bottomMargin != desiredMargin) {
                    params.bottomMargin = desiredMargin;
                    fragmentContainer.setLayoutParams(params);
                }
            });
            return insets;
        });

        bottomAppBar.post(() -> {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
            int desiredMargin = bottomAppBar.getHeight();
            if (params.bottomMargin != desiredMargin) {
                params.bottomMargin = desiredMargin;
                fragmentContainer.setLayoutParams(params);
            }
        });
    }
}
