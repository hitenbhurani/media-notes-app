package com.hiten.medianotesapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hiten.medianotesapp.fragments.ActivityFragment;
import com.hiten.medianotesapp.fragments.FavoritesFragment;
import com.hiten.medianotesapp.fragments.HomeFragment;
import com.hiten.medianotesapp.fragments.SettingsFragment;

public class DashboardActivity extends AppCompatActivity {

    private Fragment homeFragment, favoritesFragment, activityFragment, settingsFragment;
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment active;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        BottomNavigationView navView = findViewById(R.id.bottomNavigationView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            favoritesFragment = new FavoritesFragment();
            activityFragment = new ActivityFragment();
            settingsFragment = new SettingsFragment();

            fm.beginTransaction().add(R.id.fragment_container, settingsFragment, "4").hide(settingsFragment).commit();
            fm.beginTransaction().add(R.id.fragment_container, activityFragment, "3").hide(activityFragment).commit();
            fm.beginTransaction().add(R.id.fragment_container, favoritesFragment, "2").hide(favoritesFragment).commit();
            fm.beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit();
            active = homeFragment;
        } else {
            homeFragment = fm.findFragmentByTag("1");
            favoritesFragment = fm.findFragmentByTag("2");
            activityFragment = fm.findFragmentByTag("3");
            settingsFragment = fm.findFragmentByTag("4");

            if (homeFragment == null) homeFragment = new HomeFragment();
            if (favoritesFragment == null) favoritesFragment = new FavoritesFragment();
            if (activityFragment == null) activityFragment = new ActivityFragment();
            if (settingsFragment == null) settingsFragment = new SettingsFragment();

            if (fm.findFragmentByTag("1") == null) {
                fm.beginTransaction().add(R.id.fragment_container, settingsFragment, "4").hide(settingsFragment).commit();
                fm.beginTransaction().add(R.id.fragment_container, activityFragment, "3").hide(activityFragment).commit();
                fm.beginTransaction().add(R.id.fragment_container, favoritesFragment, "2").hide(favoritesFragment).commit();
                fm.beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit();
            }
            
            // Determine which was active
            active = homeFragment;
            if (homeFragment != null && homeFragment.isHidden()) {
                if (favoritesFragment != null && !favoritesFragment.isHidden()) active = favoritesFragment;
                else if (activityFragment != null && !activityFragment.isHidden()) active = activityFragment;
                else if (settingsFragment != null && !settingsFragment.isHidden()) active = settingsFragment;
            }
        }

        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment next = null;
            if (itemId == R.id.nav_home) next = homeFragment;
            else if (itemId == R.id.nav_favorites) next = favoritesFragment;
            else if (itemId == R.id.nav_notifications) next = activityFragment;
            else if (itemId == R.id.nav_settings) next = settingsFragment;

            if (next != null && next != active) {
                fm.beginTransaction().hide(active).show(next).commit();
                active = next;
                return true;
            }
            return false;
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }
}
