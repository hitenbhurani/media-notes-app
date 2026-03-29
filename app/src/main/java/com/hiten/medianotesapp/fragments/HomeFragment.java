package com.hiten.medianotesapp.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.hiten.medianotesapp.R;
import com.hiten.medianotesapp.adapters.NotesAdapter;
import com.hiten.medianotesapp.database.NoteRepository;
import com.hiten.medianotesapp.model.Note;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvNotes;
    private NotesAdapter adapter;
    private FirebaseAuth auth;

    private SwipeRefreshLayout swipeRefresh;
    private SearchView searchView;
    private ChipGroup chipGroup;
    private View emptyState;

    private final List<Note> fullList = new ArrayList<>();
    private NoteRepository noteRepository;

    private String currentQuery = "";
    private String currentFilter = "All";

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        auth = FirebaseAuth.getInstance();
        noteRepository = NoteRepository.getInstance(requireContext());

        rvNotes = view.findViewById(R.id.rvNotes);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        searchView = view.findViewById(R.id.searchView);
        chipGroup = view.findViewById(R.id.chipGroupFilter);
        emptyState = view.findViewById(R.id.emptyState);

        setupRecyclerView();
        setupSearch();
        setupFilter();
        setupSwipe();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new NotesAdapter(new ArrayList<>());
        rvNotes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotes.setAdapter(adapter);
    }

    private void loadNotes() {
        if (auth.getCurrentUser() == null) {
            fullList.clear();
            applyFilters();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }

        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        noteRepository.getAllNotes(auth.getCurrentUser().getUid(), new NoteRepository.DataCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> data) {
                if (!isAdded()) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                fullList.clear();
                fullList.addAll(data);
                applyFilters();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                applyFilters();
            }
        });
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
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                currentFilter = "All";
                applyFilters();
                return;
            }
            Chip chip = group.findViewById(checkedId);
            currentFilter = chip != null ? chip.getText().toString() : "All";
            applyFilters();
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

    private void applyFilters() {
        List<Note> filtered = new ArrayList<>();
        String query = currentQuery != null ? currentQuery.trim().toLowerCase() : "";
        String filter = currentFilter != null ? currentFilter.trim() : "All";

        for (Note note : fullList) {
            if (note == null) continue;

            boolean matchesQuery =
                    TextUtils.isEmpty(query)
                            || (note.getTitle() != null && note.getTitle().toLowerCase().contains(query))
                            || (note.getDescription() != null && note.getDescription().toLowerCase().contains(query));

            String noteType = note.getNoteType();
            boolean matchesType;
            if (filter.equalsIgnoreCase("All")) {
                matchesType = true;
            } else if (filter.equalsIgnoreCase("Others")) {
                matchesType = noteType != null && noteType.toLowerCase().startsWith("others");
            } else {
                matchesType = noteType != null && noteType.equalsIgnoreCase(filter);
            }

            if (matchesQuery && matchesType) {
                filtered.add(note);
            }
        }

        updateUI(filtered);
    }

    private void updateUI(List<Note> list) {
        adapter.updateList(list);

        if (list.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvNotes.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvNotes.setVisibility(View.VISIBLE);
        }
    }
}
