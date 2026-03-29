package com.hiten.medianotesapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.hiten.medianotesapp.R;
import com.hiten.medianotesapp.adapters.NotesAdapter;
import com.hiten.medianotesapp.database.NoteRepository;
import com.hiten.medianotesapp.model.Note;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView rvFavorites;
    private NotesAdapter adapter;

    private FirebaseAuth auth;
    private NoteRepository noteRepository;

    private View emptyState;

    public FavoritesFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        auth = FirebaseAuth.getInstance();
        noteRepository = NoteRepository.getInstance(requireContext());

        rvFavorites = view.findViewById(R.id.rvFavorites);
        emptyState = view.findViewById(R.id.emptyState);

        setupRecycler();
        return view;
    }

    private void setupRecycler() {
        adapter = new NotesAdapter(new ArrayList<>());
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        if (auth.getCurrentUser() == null) {
            updateUI(new ArrayList<>());
            return;
        }

        noteRepository.getFavoriteNotes(auth.getCurrentUser().getUid(), new NoteRepository.DataCallback<List<Note>>() {
            @Override
            public void onSuccess(List<Note> data) {
                if (!isAdded()) return;
                updateUI(data);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                updateUI(new ArrayList<>());
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void updateUI(List<Note> list) {
        adapter.updateList(list);

        if (list.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvFavorites.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvFavorites.setVisibility(View.VISIBLE);
        }
    }
}
