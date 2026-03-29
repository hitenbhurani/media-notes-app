package com.hiten.medianotesapp.database;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.hiten.medianotesapp.model.Note;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }

    private static volatile NoteRepository instance;

    private final NoteDao noteDao;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private NoteRepository(Context context) {
        this.noteDao = new NoteDao(new DBHelper(context.getApplicationContext()));
        this.executor = Executors.newFixedThreadPool(4); // Use multiple threads for better responsiveness
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static NoteRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (NoteRepository.class) {
                if (instance == null) {
                    instance = new NoteRepository(context);
                }
            }
        }
        return instance;
    }

    public void insertNote(Note note, DataCallback<Long> callback) {
        executor.execute(() -> {
            try {
                long id = noteDao.insertNote(note);
                mainHandler.post(() -> { if(callback != null) callback.onSuccess(id); });
            } catch (Exception e) {
                mainHandler.post(() -> { if(callback != null) callback.onError(e); });
            }
        });
    }

    public void updateNote(Note note, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                noteDao.updateNote(note);
                mainHandler.post(() -> { if(callback != null) callback.onSuccess(); });
            } catch (Exception e) {
                mainHandler.post(() -> { if(callback != null) callback.onError(e); });
            }
        });
    }

    public void deleteNote(Note note, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                noteDao.deleteNote(note);
                mainHandler.post(() -> { if(callback != null) callback.onSuccess(); });
            } catch (Exception e) {
                mainHandler.post(() -> { if(callback != null) callback.onError(e); });
            }
        });
    }

    public void getAllNotes(String userId, DataCallback<List<Note>> callback) {
        executor.execute(() -> {
            try {
                List<Note> notes = noteDao.getAllNotes(userId);
                mainHandler.post(() -> { if(callback != null) callback.onSuccess(notes); });
            } catch (Exception e) {
                mainHandler.post(() -> { if(callback != null) callback.onError(e); });
            }
        });
    }

    public void updateFavoriteStatus(String noteId, int isFavorite, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                noteDao.updateFavoriteStatus(noteId, isFavorite);
                mainHandler.post(() -> { if(callback != null) callback.onSuccess(); });
            } catch (Exception e) {
                mainHandler.post(() -> { if(callback != null) callback.onError(e); });
            }
        });
    }

    public void updateDoneStatus(String noteId, int isDone, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                noteDao.updateDoneStatus(noteId, isDone);
                mainHandler.post(() -> { if(callback != null) callback.onSuccess(); });
            } catch (Exception e) {
                mainHandler.post(() -> { if(callback != null) callback.onError(e); });
            }
        });
    }

    public void getFavoriteNotes(String userId, DataCallback<List<Note>> callback) {
        executor.execute(() -> {
            try {
                List<Note> notes = noteDao.getFavoriteNotes(userId);
                mainHandler.post(() -> { if(callback != null) callback.onSuccess(notes); });
            } catch (Exception e) {
                mainHandler.post(() -> { if(callback != null) callback.onError(e); });
            }
        });
    }

    public void deleteAllNotes(String userId, DataCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                int count = noteDao.deleteAllNotes(userId);
                mainHandler.post(() -> { if(callback != null) callback.onSuccess(count); });
            } catch (Exception e) {
                mainHandler.post(() -> { if(callback != null) callback.onError(e); });
            }
        });
    }

    public void getNoteById(String noteId, String userId, DataCallback<Note> callback) {
        executor.execute(() -> {
            try {
                Note note = noteDao.getNoteById(noteId, userId);
                mainHandler.post(() -> { if(callback != null) callback.onSuccess(note); });
            } catch (Exception e) {
                mainHandler.post(() -> { if(callback != null) callback.onError(e); });
            }
        });
    }
}
