package com.hiten.medianotesapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hiten.medianotesapp.adapters.NotesAdapter;
import com.hiten.medianotesapp.database.DBHelper;
import com.hiten.medianotesapp.model.Note;

import java.util.List;

public class NotesActivity extends AppCompatActivity implements SensorEventListener {

    private RecyclerView rvNotes;
    private NotesAdapter adapter;
    private DBHelper dbHelper;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private static final float SHAKE_THRESHOLD = 12.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        rvNotes = findViewById(R.id.rvNotes);
        dbHelper = new DBHelper(this);

        setupRecyclerView();
        setupSensor();
    }

    private void setupRecyclerView() {
        List<Note> noteList = dbHelper.getAllNotes();
        adapter = new NotesAdapter(noteList);
        rvNotes.setLayoutManager(new LinearLayoutManager(this));
        rvNotes.setAdapter(adapter);
    }

    private void setupSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void refreshNotes() {
        List<Note> noteList = dbHelper.getAllNotes();
        adapter = new NotesAdapter(noteList);
        rvNotes.setAdapter(adapter);
        Toast.makeText(this, "Notes refreshed", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}
