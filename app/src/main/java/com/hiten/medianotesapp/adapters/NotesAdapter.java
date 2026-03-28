package com.hiten.medianotesapp.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.hiten.medianotesapp.NoteDetailActivity;
import com.hiten.medianotesapp.R;
import com.hiten.medianotesapp.database.DBHelper;
import com.hiten.medianotesapp.model.Note;
import java.io.File;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> noteList;
    private DBHelper dbHelper;

    public NotesAdapter(List<Note> noteList, DBHelper dbHelper) {
        this.noteList = noteList;
        this.dbHelper = dbHelper;
    }

    public void updateList(List<Note> newList) {
        this.noteList = newList;
        notifyDataSetChanged();
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
        
        setupCategoryChip(holder.tvNoteType, note.getNoteType());

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

        // Fix: Show star ONLY if favorite. Remove auto-toggle.
        if (note.getIsFavorite() == 1) {
            holder.ivFavorite.setVisibility(View.VISIBLE);
            holder.ivFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            holder.ivFavorite.setVisibility(View.GONE);
        }

        // Open Detail Screen
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), NoteDetailActivity.class);
            intent.putExtra("id", note.getId());
            intent.putExtra("title", note.getTitle());
            intent.putExtra("description", note.getDescription());
            intent.putExtra("image_path", note.getImagePath());
            intent.putExtra("note_type", note.getNoteType());
            intent.putExtra("date", note.getDate());
            intent.putExtra("is_favorite", note.getIsFavorite());
            v.getContext().startActivity(intent);
        });
    }

    private void setupCategoryChip(TextView textView, String type) {
        if (type == null || type.isEmpty()) type = "General";
        textView.setText(type);
        
        int bgColor;
        int textColor;

        switch (type.toLowerCase()) {
            case "work":
                bgColor = ContextCompat.getColor(textView.getContext(), R.color.chip_work_bg);
                textColor = ContextCompat.getColor(textView.getContext(), R.color.chip_work_text);
                break;
            case "study":
                bgColor = ContextCompat.getColor(textView.getContext(), R.color.chip_study_bg);
                textColor = ContextCompat.getColor(textView.getContext(), R.color.chip_study_text);
                break;
            case "personal":
                bgColor = ContextCompat.getColor(textView.getContext(), R.color.chip_personal_bg);
                textColor = ContextCompat.getColor(textView.getContext(), R.color.chip_personal_text);
                break;
            default:
                bgColor = ContextCompat.getColor(textView.getContext(), R.color.chip_default_bg);
                textColor = ContextCompat.getColor(textView.getContext(), R.color.chip_default_text);
                break;
        }

        textView.setTextColor(textColor);
        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(textView.getContext(), R.drawable.bg_tag);
        if (drawable != null) {
            drawable.mutate();
            drawable.setColor(bgColor);
            textView.setBackground(drawable);
        }
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDate, tvNoteType;
        ImageView ivThumbnail, ivFavorite;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvDescription = itemView.findViewById(R.id.tvNoteDescription);
            tvDate = itemView.findViewById(R.id.tvNoteDate);
            tvNoteType = itemView.findViewById(R.id.tvNoteType);
            ivThumbnail = itemView.findViewById(R.id.ivNoteThumbnail);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
        }
    }
}
