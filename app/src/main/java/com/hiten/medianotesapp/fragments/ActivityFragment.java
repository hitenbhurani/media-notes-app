package com.hiten.medianotesapp.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.hiten.medianotesapp.R;
import com.hiten.medianotesapp.adapters.ActivityAdapter;
import com.hiten.medianotesapp.database.NoteRepository;
import com.hiten.medianotesapp.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityFragment extends Fragment implements ActivityAdapter.OnNoteActionListener {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private RecyclerView rvActivity;
    private ActivityAdapter adapter;
    private View scrollActivity;
    private View layoutOverview;
    private MaterialCardView cardChart;
    private MaterialCardView cardStreak;
    private View emptyState;
    private TextView tvStatTotal;
    private TextView tvStatWeek;
    private TextView tvStatDone;
    private TextView tvStatPending;
    private TextView tvStreak;
    private BarChart barChartWeekly;
    private CalendarView calendarView;
    private TextView tvSelectedDay;
    private TextView tvMarkedDays;

    private FirebaseAuth auth;
    private NoteRepository noteRepository;
    private final ExecutorService computeExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final List<Note> allNotes = new ArrayList<>();
    private final Map<String, List<Note>> notesByDay = new HashMap<>();
    private final Set<String> markedDays = new HashSet<>();
    private String selectedDayKey = "";

    public ActivityFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        auth = FirebaseAuth.getInstance();
        noteRepository = NoteRepository.getInstance(requireContext());

        rvActivity = view.findViewById(R.id.rvActivity);
        scrollActivity = view.findViewById(R.id.scrollActivity);
        layoutOverview = view.findViewById(R.id.layoutOverview);
        cardChart = view.findViewById(R.id.cardChart);
        cardStreak = view.findViewById(R.id.cardStreak);
        emptyState = view.findViewById(R.id.emptyStateActivity);
        tvStatTotal = view.findViewById(R.id.tvStatTotal);
        tvStatWeek = view.findViewById(R.id.tvStatWeek);
        tvStatDone = view.findViewById(R.id.tvStatDone);
        tvStatPending = view.findViewById(R.id.tvStatPending);
        tvStreak = view.findViewById(R.id.tvStreak);
        barChartWeekly = view.findViewById(R.id.barChartWeekly);
        calendarView = view.findViewById(R.id.calendarViewActivity);
        tvSelectedDay = view.findViewById(R.id.tvSelectedDay);
        tvMarkedDays = view.findViewById(R.id.tvMarkedDays);

        setupRecycler();
        setupCalendar();
        setupChart();

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
        updateSelectedDayLabel(current);

        calendarView.setOnDateChangeListener((calendar, year, month, dayOfMonth) -> {
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
            updateSelectedDayLabel(selectedDate);
            updateDailyListFromMemory();
        });

        calendarView.setOnLongClickListener(v -> {
            showSelectedDayPreviewDialog();
            return true;
        });
    }

    private void setupChart() {
        barChartWeekly.getDescription().setEnabled(false);
        barChartWeekly.getLegend().setEnabled(false);
        barChartWeekly.setDrawGridBackground(false);
        barChartWeekly.setNoDataText("No activity yet");
        barChartWeekly.setPinchZoom(false);
        barChartWeekly.setDoubleTapToZoomEnabled(false);

        int axisTextColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface);
        XAxis xAxis = barChartWeekly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(axisTextColor);

        YAxis axisLeft = barChartWeekly.getAxisLeft();
        axisLeft.setAxisMinimum(0f);
        axisLeft.setDrawGridLines(false);
        axisLeft.setTextColor(axisTextColor);

        YAxis axisRight = barChartWeekly.getAxisRight();
        axisRight.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            loadNotes();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isResumed()) {
            loadNotes();
        }
    }

    private void loadNotes() {
        if (auth.getCurrentUser() == null) {
            allNotes.clear();
            rebuildAndRenderDashboard();
            return;
        }

        noteRepository.getAllNotes(auth.getCurrentUser().getUid(), new NoteRepository.DataCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> data) {
                if (!isAdded()) {
                    return;
                }

                allNotes.clear();
                allNotes.addAll(data);
                rebuildAndRenderDashboard();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(), "Failed to load activity", Toast.LENGTH_SHORT).show();
                allNotes.clear();
                rebuildAndRenderDashboard();
            }
        });
    }

    private void rebuildAndRenderDashboard() {
        final List<Note> snapshot = new ArrayList<>(allNotes);
        final String dayKey = selectedDayKey;

        computeExecutor.execute(() -> {
            DashboardState state = buildDashboardState(snapshot, dayKey);
            mainHandler.post(() -> {
                if (!isAdded()) {
                    return;
                }
                applyDashboardState(state);
            });
        });
    }

    private DashboardState buildDashboardState(List<Note> notes, String currentSelectedDayKey) {
        DashboardState state = new DashboardState();
        state.total = notes.size();

        Calendar weekStart = Calendar.getInstance();
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);
        long weekStartMillis = weekStart.getTimeInMillis();

        Map<String, Integer> notesPerDay = new HashMap<>();
        TreeSet<Long> activeDayStarts = new TreeSet<>();

        for (Note note : notes) {
            if (note == null) {
                continue;
            }

            if (note.getIsDone() == 1) {
                state.doneCount++;
            } else {
                state.pendingCount++;
            }

            Date ts = note.getTimestamp();
            if (ts == null) {
                continue;
            }

            String key = toDayKey(ts);
            List<Note> bucket = state.notesByDay.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                state.notesByDay.put(key, bucket);
            }
            bucket.add(note);
            state.markedDays.add(key);

            notesPerDay.put(key, notesPerDay.getOrDefault(key, 0) + 1);
            long dayStart = getStartOfDayMillis(ts.getTime());
            activeDayStarts.add(dayStart);

            if (dayStart >= weekStartMillis) {
                state.weekCount++;
            }
        }

        if (TextUtils.isEmpty(currentSelectedDayKey)) {
            currentSelectedDayKey = toDayKey(new Date());
        }
        state.selectedDayKey = currentSelectedDayKey;

        List<Note> selected = state.notesByDay.get(state.selectedDayKey);
        if (selected != null) {
            selected.sort((a, b) -> {
                Date ta = a.getTimestamp();
                Date tb = b.getTimestamp();
                long va = ta != null ? ta.getTime() : 0L;
                long vb = tb != null ? tb.getTime() : 0L;
                return Long.compare(vb, va);
            });
            state.selectedDayNotes.addAll(selected);
        }

        state.streakDays = calculateStreak(activeDayStarts);
        buildWeeklySeries(state, notesPerDay);
        state.markedDaysSummary = buildMarkedDaysSummary(state.markedDays);
        state.hasNotes = !notes.isEmpty();

        return state;
    }

    private int calculateStreak(TreeSet<Long> activeDayStarts) {
        if (activeDayStarts.isEmpty()) {
            return 0;
        }

        int streak = 0;
        long cursor = activeDayStarts.last();
        while (activeDayStarts.contains(cursor)) {
            streak++;
            cursor -= DAY_MILLIS;
        }
        return streak;
    }

    private void buildWeeklySeries(DashboardState state, Map<String, Integer> notesPerDay) {
        SimpleDateFormat dayLabelFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        long todayStart = getStartOfDayMillis(System.currentTimeMillis());

        for (int i = 6; i >= 0; i--) {
            long millis = todayStart - (long) i * DAY_MILLIS;
            String key = toDayKey(new Date(millis));
            state.chartLabels.add(dayLabelFormat.format(new Date(millis)));
            state.chartCounts.add(notesPerDay.getOrDefault(key, 0));
        }
    }

    private String buildMarkedDaysSummary(Set<String> dayKeys) {
        if (dayKeys.isEmpty()) {
            return "Active dates: none";
        }

        List<String> keys = new ArrayList<>(dayKeys);
        Collections.sort(keys, Collections.reverseOrder());
        int max = Math.min(8, keys.size());

        StringBuilder sb = new StringBuilder("Active dates: ");
        for (int i = 0; i < max; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(keys.get(i));
        }
        if (keys.size() > max) {
            sb.append(" +").append(keys.size() - max).append(" more");
        }
        return sb.toString();
    }

    private String toDayKey(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    private Date fromDayKey(String key) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(key);
        } catch (Exception e) {
            return new Date();
        }
    }

    private String formatDisplayDate(Date date) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
    }

    private long getStartOfDayMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void applyDashboardState(DashboardState state) {
        notesByDay.clear();
        notesByDay.putAll(state.notesByDay);

        markedDays.clear();
        markedDays.addAll(state.markedDays);

        selectedDayKey = state.selectedDayKey;

        tvStatTotal.setText(String.valueOf(state.total));
        tvStatWeek.setText(String.valueOf(state.weekCount));
        tvStatDone.setText(String.valueOf(state.doneCount));
        tvStatPending.setText(String.valueOf(state.pendingCount));
        tvStreak.setText("You've been active for " + state.streakDays + " days");
        tvMarkedDays.setText(state.markedDaysSummary);

        Date selectedDate = fromDayKey(selectedDayKey);
        updateSelectedDayLabel(selectedDate);

        adapter.updateList(state.selectedDayNotes);
        tvSelectedDay.setTextColor(markedDays.contains(selectedDayKey)
            ? resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)
                : resolveThemeColor(com.google.android.material.R.attr.colorOnSurface));

        renderChart(state.chartLabels, state.chartCounts);

        int visibleSections = state.hasNotes ? View.VISIBLE : View.GONE;
        scrollActivity.setVisibility(state.hasNotes ? View.VISIBLE : View.GONE);
        layoutOverview.setVisibility(visibleSections);
        cardChart.setVisibility(visibleSections);
        cardStreak.setVisibility(visibleSections);
        emptyState.setVisibility(state.hasNotes ? View.GONE : View.VISIBLE);
    }

    private void renderChart(List<String> labels, List<Integer> counts) {
        if (labels.isEmpty() || counts.isEmpty()) {
            barChartWeekly.clear();
            barChartWeekly.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < counts.size(); i++) {
            entries.add(new BarEntry(i, counts.get(i)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Notes");
        dataSet.setColor(resolveThemeColor(androidx.appcompat.R.attr.colorPrimary));
        dataSet.setValueTextColor(resolveThemeColor(com.google.android.material.R.attr.colorOnSurface));
        dataSet.setValueTextSize(11f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.55f);

        XAxis xAxis = barChartWeekly.getXAxis();
        xAxis.setLabelCount(labels.size(), false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        barChartWeekly.setData(barData);
        barChartWeekly.setFitBars(true);
        barChartWeekly.animateY(700);
        barChartWeekly.invalidate();
    }

    private void updateSelectedDayLabel(Date date) {
        List<Note> selected = notesByDay.get(selectedDayKey);
        int count = selected == null ? 0 : selected.size();
        tvSelectedDay.setText("Notes on " + formatDisplayDate(date) + " (" + count + ")");
    }

    private void updateDailyListFromMemory() {
        List<Note> selected = notesByDay.get(selectedDayKey);
        if (selected == null) {
            selected = new ArrayList<>();
        } else {
            selected = new ArrayList<>(selected);
        }

        selected.sort((a, b) -> {
            Date ta = a.getTimestamp();
            Date tb = b.getTimestamp();
            long va = ta != null ? ta.getTime() : 0L;
            long vb = tb != null ? tb.getTime() : 0L;
            return Long.compare(vb, va);
        });

        adapter.updateList(selected);
        updateSelectedDayLabel(fromDayKey(selectedDayKey));
        tvSelectedDay.setTextColor(markedDays.contains(selectedDayKey)
            ? resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)
                : resolveThemeColor(com.google.android.material.R.attr.colorOnSurface));
    }

    private void showSelectedDayPreviewDialog() {
        List<Note> selected = notesByDay.get(selectedDayKey);
        if (selected == null || selected.isEmpty()) {
            Toast.makeText(requireContext(), "No notes on this day", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        int max = Math.min(5, selected.size());
        for (int i = 0; i < max; i++) {
            Note note = selected.get(i);
            String title = TextUtils.isEmpty(note.getTitle()) ? "Untitled note" : note.getTitle();
            sb.append("- ").append(title).append(" • ")
                    .append(note.getIsDone() == 1 ? "Done" : "Pending").append("\n");
        }
        if (selected.size() > max) {
            sb.append("+ ").append(selected.size() - max).append(" more");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Preview for " + formatDisplayDate(fromDayKey(selectedDayKey)))
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private int resolveThemeColor(int attrRes) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }

    @Override
    public void onToggleDone(Note note) {
        if (note == null || TextUtils.isEmpty(note.getId())) {
            return;
        }

        int nextDone = note.getIsDone() == 1 ? 0 : 1;
        note.setIsDone(nextDone);
        rebuildAndRenderDashboard();

        noteRepository.updateDoneStatus(note.getId(), nextDone, new NoteRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Exception e) {
                note.setIsDone(nextDone == 1 ? 0 : 1);
                rebuildAndRenderDashboard();
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to update task state", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onPreview(Note note) {
        if (note == null || !isAdded()) {
            return;
        }

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        computeExecutor.shutdownNow();
    }

    private static class DashboardState {
        int total;
        int weekCount;
        int doneCount;
        int pendingCount;
        int streakDays;
        boolean hasNotes;
        String selectedDayKey;
        String markedDaysSummary;
        final Map<String, List<Note>> notesByDay = new HashMap<>();
        final Set<String> markedDays = new HashSet<>();
        final List<Note> selectedDayNotes = new ArrayList<>();
        final List<String> chartLabels = new ArrayList<>();
        final List<Integer> chartCounts = new ArrayList<>();
    }
}
