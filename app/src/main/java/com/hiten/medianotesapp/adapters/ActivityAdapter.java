package com.hiten.medianotesapp.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.hiten.medianotesapp.R;
import com.hiten.medianotesapp.model.Note;
import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    public interface OnNoteActionListener {
        void onToggleDone(Note note);
        void onPreview(Note note);
    }

    private List<Note> noteList;
    private final OnNoteActionListener listener;

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
            holder.tvAction.setText("Type: General • Pending - tap to mark done");
            holder.tvTime.setText("Just now");
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
            return;
        }

        String title = note.getTitle() != null && !note.getTitle().trim().isEmpty() ? note.getTitle() : "Untitled note";
        holder.tvTitle.setText(title);

        String type = note.getNoteType() == null || note.getNoteType().trim().isEmpty() ? "General" : note.getNoteType();
        String doneText = note.getIsDone() == 1 ? "Done - tap to mark pending" : "Pending - tap to mark done";
        String description = note.getDescription() == null || note.getDescription().trim().isEmpty() ? "No description" : note.getDescription().trim();
        holder.tvAction.setText("Type: " + type + " • " + doneText + "\n" + description);

        if (note.getTimestamp() != null) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    note.getTimestamp().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("Just now");
        }

        if (note.getIsDone() == 1) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.chip_study_bg));
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
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
        TextView tvTitle, tvAction, tvTime;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvLogTitle);
            tvAction = itemView.findViewById(R.id.tvLogAction);
            tvTime = itemView.findViewById(R.id.tvLogTime);
        }
    }
}
