package com.hiten.medianotesapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hiten.medianotesapp.model.Note;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notes_local.db";
    private static final int DATABASE_VERSION = 3;
    private static final String TABLE_NOTES = "notes";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_NOTE_TYPE = "note_type";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_IS_FAVORITE = "is_favorite";
    private static final String COLUMN_IS_DONE = "is_done";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String create = "CREATE TABLE IF NOT EXISTS " + TABLE_NOTES + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT NOT NULL,"
                + COLUMN_DESCRIPTION + " TEXT,"
                + COLUMN_NOTE_TYPE + " TEXT,"
                + COLUMN_IMAGE_PATH + " TEXT,"
                + COLUMN_USER_ID + " TEXT NOT NULL,"
                + COLUMN_TIMESTAMP + " INTEGER,"
                + COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0,"
                + COLUMN_IS_DONE + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(create);
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_notes_user_ts ON " + TABLE_NOTES + "(" + COLUMN_USER_ID + "," + COLUMN_TIMESTAMP + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
            onCreate(db);
        }
    }

    public long insertNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_DESCRIPTION, note.getDescription());
        values.put(COLUMN_IMAGE_PATH, note.getImagePath());
        values.put(COLUMN_NOTE_TYPE, note.getNoteType());
        values.put(COLUMN_USER_ID, note.getUserId());
        values.put(COLUMN_TIMESTAMP, note.getTimestamp() != null ? note.getTimestamp().getTime() : System.currentTimeMillis());
        values.put(COLUMN_IS_FAVORITE, note.getIsFavorite());
        values.put(COLUMN_IS_DONE, note.getIsDone());
        return db.insert(TABLE_NOTES, null, values);
    }

    public int updateNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, note.getTitle());
        values.put(COLUMN_DESCRIPTION, note.getDescription());
        values.put(COLUMN_IMAGE_PATH, note.getImagePath());
        values.put(COLUMN_NOTE_TYPE, note.getNoteType());
        values.put(COLUMN_USER_ID, note.getUserId());
        values.put(COLUMN_TIMESTAMP, note.getTimestamp() != null ? note.getTimestamp().getTime() : System.currentTimeMillis());
        values.put(COLUMN_IS_FAVORITE, note.getIsFavorite());
        values.put(COLUMN_IS_DONE, note.getIsDone());
        return db.update(TABLE_NOTES, values, COLUMN_ID + "=?", new String[]{note.getId()});
    }

    public int deleteNote(Note note) {
        if (note == null || note.getId() == null) return 0;
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NOTES, COLUMN_ID + "=?", new String[]{note.getId()});
    }

    public int deleteAllNotes(String userId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_NOTES, COLUMN_USER_ID + "=?", new String[]{userId});
    }

    public int updateDoneStatus(String noteId, int isDone) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DONE, isDone);
        return db.update(TABLE_NOTES, values, COLUMN_ID + "=?", new String[]{noteId});
    }

    public int updateFavoriteStatus(String noteId, int isFavorite) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_FAVORITE, isFavorite);
        return db.update(TABLE_NOTES, values, COLUMN_ID + "=?", new String[]{noteId});
    }

    public Note getNoteById(String noteId, String userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NOTES + " WHERE " + COLUMN_ID + "=? AND " + COLUMN_USER_ID + "=? LIMIT 1",
                new String[]{noteId, userId}
        );
        try {
            if (cursor.moveToFirst()) {
                return mapNote(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public List<Note> getAllNotes(String userId) {
        return getNotesByQuery(
                "SELECT * FROM " + TABLE_NOTES + " WHERE " + COLUMN_USER_ID + "=? ORDER BY " + COLUMN_TIMESTAMP + " DESC",
                new String[]{userId}
        );
    }

    public List<Note> getFavoriteNotes(String userId) {
        return getNotesByQuery(
                "SELECT * FROM " + TABLE_NOTES + " WHERE " + COLUMN_USER_ID + "=? AND " + COLUMN_IS_FAVORITE + "=1 ORDER BY " + COLUMN_TIMESTAMP + " DESC",
                new String[]{userId}
        );
    }

    public List<Note> getNotesByDate(String userId, long startMillis, long endMillis) {
        return getNotesByQuery(
                "SELECT * FROM " + TABLE_NOTES + " WHERE " + COLUMN_USER_ID + "=? AND " + COLUMN_TIMESTAMP + ">=? AND " + COLUMN_TIMESTAMP + "<=? ORDER BY " + COLUMN_TIMESTAMP + " DESC",
                new String[]{userId, String.valueOf(startMillis), String.valueOf(endMillis)}
        );
    }

    private List<Note> getNotesByQuery(String query, String[] args) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, args);
        try {
            if (cursor.moveToFirst()) {
                do {
                    notes.add(mapNote(cursor));
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
        return notes;
    }

    private Note mapNote(Cursor cursor) {
        Note note = new Note();
        note.setId(String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))));
        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
        note.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
        note.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));
        note.setNoteType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE_TYPE)));
        note.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)));

        long ts = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
        note.setTimestamp(ts > 0 ? new Date(ts) : new Date());

        note.setIsFavorite(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_FAVORITE)));
        note.setIsDone(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DONE)));
        return note;
    }
}
