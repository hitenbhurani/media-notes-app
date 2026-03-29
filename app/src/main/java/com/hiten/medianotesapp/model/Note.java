package com.hiten.medianotesapp.model;

import java.util.Date;

public class Note {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String imagePath;
    private String noteType;
    private int isFavorite; // 0 or 1
    private int isDone; // 0 or 1

    private Date timestamp;

    public Note() {}

    public Note(String title, String description, String imagePath, String noteType, String userId) {
        this.title = title;
        this.description = description;
        this.imagePath = imagePath;
        this.noteType = noteType;
        this.userId = userId;
        this.isFavorite = 0;
        this.isDone = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    // Backward-compatible aliases used by existing UI binding code.
    public String getImageUrl() { return imagePath; }
    public void setImageUrl(String imageUrl) { this.imagePath = imageUrl; }

    public String getNoteType() { return noteType; }
    public void setNoteType(String noteType) { this.noteType = noteType; }

    public int getIsFavorite() { return isFavorite; }
    public void setIsFavorite(int isFavorite) { this.isFavorite = isFavorite; }

    public int getIsDone() { return isDone; }
    public void setIsDone(int isDone) { this.isDone = isDone; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getDateFormatted() {
        if (timestamp == null) return "Just now";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
        return sdf.format(timestamp);
    }
}
