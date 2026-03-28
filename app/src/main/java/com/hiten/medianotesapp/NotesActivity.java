package com.hiten.medianotesapp;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.hiten.medianotesapp.adapters.NotesAdapter;
import com.hiten.medianotesapp.database.DBHelper;
import com.hiten.medianotesapp.model.Note;

import java.util.List;

public class NotesActivity extends AppCompatActivity implements SensorEventListener {

    private RecyclerView rvNotes;
    private NotesAdapter adapter;
    private DBHelper dbHelper;
    private SearchView searchView;
    private ChipGroup chipGroupFilter;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyState;
    private View coordinatorLayout;
    private FloatingActionButton fabAddNote;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private static final float SHAKE_THRESHOLD = 12.0f;

    private String currentQuery = "";
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        dbHelper = new DBHelper(this);
        rvNotes = findViewById(R.id.rvNotes);
        searchView = findViewById(R.id.searchView);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyState = findViewById(R.id.emptyState);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        fabAddNote = findViewById(R.id.fabAddNote);

        setupRecyclerView();
        setupSearch();
        setupFilter();
        setupSwipeRefresh();
        setupSensor();
        setupFAB();
    }

    private void setupRecyclerView() {
        List<Note> noteList = dbHelper.getAllNotes();
        adapter = new NotesAdapter(noteList, dbHelper);
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setItemAnimator(new DefaultItemAnimator());
        rvNotes.setAdapter(adapter);
        
        checkEmptyState(noteList.size());
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                applyFilters();
                return true;
            }
        });
    }

    private void setupFilter() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = findViewById(checkedId);
            if (chip != null) {
                currentFilter = chip.getText().toString();
                applyFilters();
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.primary_deep));
        swipeRefresh.setOnRefreshListener(this::refreshNotes);
    }

    private void setupFAB() {
        fabAddNote.setOnClickListener(v -> {
            // Fix: FAB now opens MainActivity to add a new note
            Intent intent = new Intent(NotesActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void applyFilters() {
        List<Note> filteredList;
        if (!currentQuery.isEmpty()) {
            filteredList = dbHelper.searchNotes(currentQuery);
        } else {
            filteredList = dbHelper.filterNotes(currentFilter);
        }
        
        adapter.updateList(filteredList);
        checkEmptyState(filteredList.size());
    }

    private void checkEmptyState(int count) {
        if (count == 0) {
            emptyState.setVisibility(View.VISIBLE);
            rvNotes.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvNotes.setVisibility(View.VISIBLE);
        }
    }

    private void setupSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void refreshNotes() {
        applyFilters();
        swipeRefresh.setRefreshing(false);
        showSnackbar("Notes refreshed");
    }

    private void showSnackbar(String message) {
        Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_SHORT).show();
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
                refreshNotes();
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
        applyFilters();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}
