package com.hiten.medianotesapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hiten.medianotesapp.R;
import com.hiten.medianotesapp.model.Note;
import java.io.File;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> noteList;

    public NotesAdapter(List<Note> noteList) {
        this.noteList = noteList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.tvTitle.setText(note.getTitle());
        holder.tvDescription.setText(note.getDescription());
        holder.tvDate.setText(note.getDate());
        holder.tvNoteType.setText("Type: " + note.getNoteType());

        if (note.getImagePath() != null && !note.getImagePath().isEmpty()) {
            File imgFile = new File(note.getImagePath());
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                holder.ivThumbnail.setImageBitmap(myBitmap);
            } else {
                holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDate, tvNoteType;
        ImageView ivThumbnail;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvDescription = itemView.findViewById(R.id.tvNoteDescription);
            tvDate = itemView.findViewById(R.id.tvNoteDate);
            tvNoteType = itemView.findViewById(R.id.tvNoteType);
            ivThumbnail = itemView.findViewById(R.id.ivNoteThumbnail);
        }
    }
}
