package com.hiten.medianotesapp.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.hiten.medianotesapp.R;
import com.hiten.medianotesapp.adapters.ActivityAdapter;
import com.hiten.medianotesapp.database.NoteRepository;
import com.hiten.medianotesapp.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ActivityFragment extends Fragment implements ActivityAdapter.OnNoteActionListener {

    private RecyclerView rvActivity;
    private ActivityAdapter adapter;
    private View emptyState;
    private SwipeRefreshLayout swipeRefresh;
    private CalendarView calendarView;
    private TextView tvSelectedDay;
    private TextView tvUsage;
    private TextView tvMarkedDays;

    private FirebaseAuth auth;
    private NoteRepository noteRepository;

    private final List<Note> allNotes = new ArrayList<>();
    private final Map<String, List<Note>> notesByDay = new HashMap<>();
    private final Set<String> markedDays = new HashSet<>();
    private final SimpleDateFormat dayKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private String selectedDayKey = "";

    public ActivityFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        auth = FirebaseAuth.getInstance();
        noteRepository = NoteRepository.getInstance(requireContext());

        rvActivity = view.findViewById(R.id.rvActivity);
        emptyState = view.findViewById(R.id.emptyStateActivity);
        swipeRefresh = view.findViewById(R.id.swipeRefreshActivity);
        calendarView = view.findViewById(R.id.calendarViewActivity);
        tvSelectedDay = view.findViewById(R.id.tvSelectedDay);
        tvUsage = view.findViewById(R.id.tvUsageStats);
        tvMarkedDays = view.findViewById(R.id.tvMarkedDays);

        setupRecycler();
        setupCalendar();
        setupSwipe();

        return view;
    }

    private void setupRecycler() {
        adapter = new ActivityAdapter(new ArrayList<>(), this);
        rvActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActivity.setAdapter(adapter);
    }

    private void setupCalendar() {
        Date current = new Date(calendarView.getDate());
        selectedDayKey = toDayKey(current);
        tvSelectedDay.setText("Notes on " + formatDisplayDate(current));

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Date selectedDate = cal.getTime();
            selectedDayKey = toDayKey(selectedDate);
            tvSelectedDay.setText("Notes on " + formatDisplayDate(selectedDate));
            applyDateFilter();
            updateAnalytics();
        });
    }

    private void setupSwipe() {
        swipeRefresh.setOnRefreshListener(this::loadNotes);
    }

    @Override
    public void onStart() {
        super.onStart();
        loadNotes();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        if (auth.getCurrentUser() == null) {
            allNotes.clear();
            rebuildDateIndex();
            applyDateFilter();
            updateAnalytics();
            updateMarkedDaysSummary();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }

        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        noteRepository.getAllNotes(auth.getCurrentUser().getUid(), new NoteRepository.DataCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> data) {
                if (!isAdded()) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                allNotes.clear();
                allNotes.addAll(data);

                rebuildDateIndex();
                applyDateFilter();
                updateAnalytics();
                updateMarkedDaysSummary();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                updateUI(new ArrayList<>());
            }
        });
    }

    private void rebuildDateIndex() {
        notesByDay.clear();
        markedDays.clear();

        for (Note note : allNotes) {
            if (note == null || note.getTimestamp() == null) continue;

            String key = toDayKey(note.getTimestamp());
            if (!notesByDay.containsKey(key)) {
                notesByDay.put(key, new ArrayList<>());
            }
            notesByDay.get(key).add(note);
            markedDays.add(key);
        }
    }

    private void applyDateFilter() {
        List<Note> filtered = notesByDay.get(selectedDayKey);
        if (filtered == null) {
            filtered = new ArrayList<>();
        }

        int colorRes = markedDays.contains(selectedDayKey) ? R.color.accent_teal : R.color.text_primary;
        tvSelectedDay.setTextColor(requireContext().getColor(colorRes));

        updateUI(filtered);
    }

    private void updateAnalytics() {
        int total = allNotes.size();
        int weekCount = 0;

        Calendar weekStart = Calendar.getInstance();
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);
        long weekStartMillis = weekStart.getTimeInMillis();

        for (Note note : allNotes) {
            Date ts = note.getTimestamp();
            if (ts != null && ts.getTime() >= weekStartMillis) {
                weekCount++;
            }
        }

        int selectedDayCount = notesByDay.containsKey(selectedDayKey) ? notesByDay.get(selectedDayKey).size() : 0;
        tvUsage.setText("Total: " + total + " • This week: " + weekCount + " • Selected day: " + selectedDayCount);
    }

    private void updateMarkedDaysSummary() {
        if (markedDays.isEmpty()) {
            tvMarkedDays.setText("Highlighted dates: none");
            return;
        }

        List<String> keys = new ArrayList<>(markedDays);
        keys.sort(String::compareTo);

        int max = Math.min(8, keys.size());
        StringBuilder sb = new StringBuilder("Highlighted dates: ");
        for (int i = 0; i < max; i++) {
            if (i > 0) sb.append(", ");
            sb.append(keys.get(i));
        }
        if (keys.size() > max) {
            sb.append(" +").append(keys.size() - max).append(" more");
        }
        tvMarkedDays.setText(sb.toString());
    }

    private String toDayKey(Date date) {
        return dayKeyFormat.format(date);
    }

    private String formatDisplayDate(Date date) {
        return displayDayFormat.format(date);
    }

    private void updateUI(List<Note> list) {
        adapter.updateList(list);
        if (list.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvActivity.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvActivity.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onToggleDone(Note note) {
        if (note == null || TextUtils.isEmpty(note.getId())) return;

        int nextDone = note.getIsDone() == 1 ? 0 : 1;
        note.setIsDone(nextDone);
        adapter.notifyDataSetChanged();

        noteRepository.updateDoneStatus(note.getId(), nextDone, new NoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Exception e) {
                note.setIsDone(nextDone == 1 ? 0 : 1);
                adapter.notifyDataSetChanged();
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to update task state", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onPreview(Note note) {
        if (note == null || !isAdded()) return;

        String title = note.getTitle() == null ? "Untitled note" : note.getTitle();
        String description = TextUtils.isEmpty(note.getDescription()) ? "No description" : note.getDescription();
        String type = TextUtils.isEmpty(note.getNoteType()) ? "General" : note.getNoteType();
        String status = note.getIsDone() == 1 ? "Done" : "Pending";

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage("Type: " + type + "\nStatus: " + status + "\n\n" + description)
                .setPositiveButton("Close", null)
                .show();
    }
}
