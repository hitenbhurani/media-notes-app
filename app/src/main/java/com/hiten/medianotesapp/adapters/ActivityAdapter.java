package com.hiten.medianotesapp.adapters;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hiten.medianotesapp.R;
import com.hiten.medianotesapp.model.Note;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    public interface OnNoteActionListener {
        void onToggleDone(Note note);
        void onPreview(Note note);
    }

    private List<Note> noteList;
    private final OnNoteActionListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ActivityAdapter(List<Note> noteList, OnNoteActionListener listener) {
        this.noteList = noteList;
        this.listener = listener;
    }

    public void updateList(List<Note> newList) {
        this.noteList = newList != null ? newList : java.util.Collections.emptyList();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Note note = noteList.get(position);
        if (note == null) {
            holder.tvTitle.setText("Untitled note");
            holder.tvCategory.setText("General");
            holder.tvStatus.setText("Pending");
            holder.tvTime.setText("--:--");
            holder.tvStatus.setTypeface(Typeface.DEFAULT_BOLD);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
            return;
        }

        String title = !TextUtils.isEmpty(note.getTitle()) ? note.getTitle().trim() : "Untitled note";
        holder.tvTitle.setText(title);

        String type = !TextUtils.isEmpty(note.getNoteType()) ? note.getNoteType().trim() : "General";
        holder.tvCategory.setText(type);

        boolean isDone = note.getIsDone() == 1;
        holder.tvStatus.setText(isDone ? "Done" : "Pending");
        holder.tvStatus.setTypeface(Typeface.DEFAULT_BOLD);
        holder.tvStatus.setAlpha(isDone ? 0.85f : 1f);

        Date ts = note.getTimestamp();
        if (ts != null) {
            holder.tvTime.setText(timeFormat.format(ts));
        } else {
            holder.tvTime.setText("--:--");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleDone(note);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onPreview(note);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return noteList == null ? 0 : noteList.size();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvCategory;
        TextView tvStatus;
        TextView tvTime;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvLogTitle);
            tvCategory = itemView.findViewById(R.id.tvLogCategory);
            tvStatus = itemView.findViewById(R.id.tvLogStatus);
            tvTime = itemView.findViewById(R.id.tvLogTime);
        }
    }
}
