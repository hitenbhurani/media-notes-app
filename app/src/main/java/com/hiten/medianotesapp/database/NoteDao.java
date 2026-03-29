package com.hiten.medianotesapp.database;

import com.hiten.medianotesapp.model.Note;

import java.util.List;

public class NoteDao {

    private final DBHelper dbHelper;

    public NoteDao(DBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertNote(Note note) {
        return dbHelper.insertNote(note);
    }

    public int updateNote(Note note) {
        return dbHelper.updateNote(note);
    }

    public int deleteNote(Note note) {
        return dbHelper.deleteNote(note);
    }

    public int deleteAllNotes(String userId) {
        return dbHelper.deleteAllNotes(userId);
    }

    public List<Note> getAllNotes(String userId) {
        return dbHelper.getAllNotes(userId);
    }

    public List<Note> getFavoriteNotes(String userId) {
        return dbHelper.getFavoriteNotes(userId);
    }

    public List<Note> getNotesByDate(String userId, long dayStart, long dayEnd) {
        return dbHelper.getNotesByDate(userId, dayStart, dayEnd);
    }

    public int updateDoneStatus(String noteId, int isDone) {
        return dbHelper.updateDoneStatus(noteId, isDone);
    }

    public int updateFavoriteStatus(String noteId, int isFavorite) {
        return dbHelper.updateFavoriteStatus(noteId, isFavorite);
    }

    public Note getNoteById(String noteId, String userId) {
        return dbHelper.getNoteById(noteId, userId);
    }
}
