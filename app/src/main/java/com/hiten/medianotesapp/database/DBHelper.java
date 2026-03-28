package com.hiten.medianotesapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.hiten.medianotesapp.model.Note;
import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "NotesDB_8";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_NAME = "notes_8";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_NOTE_TYPE = "note_type";
    private static final String COLUMN_IS_FAVORITE = "is_favorite";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_IMAGE_PATH + " TEXT,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_NOTE_TYPE + " TEXT,"
                + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0" + ")";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0");
        }
    }

    public long insertNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_DESCRIPTION, note.getDescription());
        values.put(COLUMN_IMAGE_PATH, note.getImagePath());
        values.put(COLUMN_DATE, note.getDate());
        values.put(COLUMN_NOTE_TYPE, note.getNoteType());
        // Fix: Insert the actual favorite status from the Note object
        values.put(COLUMN_IS_FAVORITE, note.getIsFavorite());

        long id = db.insert(TABLE_NAME, null, values);
        db.close();
        return id;
    }

    public void updateNote(int id, String title, String description, String imagePath, String noteType, int isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_DESCRIPTION, description);
        values.put(COLUMN_IMAGE_PATH, imagePath);
        values.put(COLUMN_NOTE_TYPE, noteType);
        values.put(COLUMN_IS_FAVORITE, isFavorite);
        db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateFavoriteStatus(int id, int isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_FAVORITE, isFavorite);
        db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteNote(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Note> getAllNotes() {
        return getNotesByQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " DESC", null);
    }

    public List<Note> searchNotes(String query) {
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_TITLE + " LIKE ? OR " + COLUMN_DESCRIPTION + " LIKE ? ORDER BY " + COLUMN_ID + " DESC";
        return getNotesByQuery(selectQuery, new String[]{"%" + query + "%", "%" + query + "%"});
    }

    public List<Note> filterNotes(String type) {
        if (type.equalsIgnoreCase("All")) return getAllNotes();
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_NOTE_TYPE + "=? ORDER BY " + COLUMN_ID + " DESC";
        return getNotesByQuery(selectQuery, new String[]{type});
    }

    private List<Note> getNotesByQuery(String query, String[] args) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, args);

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();
                note.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                note.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                note.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));
                note.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
                note.setNoteType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TYPE)));
                note.setIsFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)));
                notes.add(note);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return notes;
    }
}
