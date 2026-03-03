package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/** Simple list of audio parts (for detail screen) with highlighting for current playing part. */
public class AudioPartsAdapter extends RecyclerView.Adapter<AudioPartsAdapter.PartHolder> {

    private final List<ServerAudioPart> parts = new ArrayList<>();
    private OnPartClickListener listener;
    private String currentPlayingPartId; // ID of currently playing part

    public interface OnPartClickListener {
        void onPartClick(ServerAudioPart part);
    }

    public void setParts(List<ServerAudioPart> list) {
        parts.clear();
        if (list != null) parts.addAll(list);
        notifyDataSetChanged();
    }

    public void setOnPartClickListener(OnPartClickListener listener) {
        this.listener = listener;
    }

    /** Set the currently playing part ID to highlight it. */
    public void setCurrentPlayingPartId(String partId) {
        String oldId = currentPlayingPartId;
        currentPlayingPartId = partId;
        // Update only affected items
        if (oldId != null) {
            for (int i = 0; i < parts.size(); i++) {
                if (parts.get(i).getId().equals(oldId)) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
        if (partId != null) {
            for (int i = 0; i < parts.size(); i++) {
                if (parts.get(i).getId().equals(partId)) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    @NonNull
    @Override
    public PartHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio_part, parent, false);
        return new PartHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PartHolder holder, int position) {
        ServerAudioPart part = parts.get(position);
        holder.partNumber.setText(String.valueOf(position + 1));
        String title = part != null ? part.getTitle() : null;
        if (title == null || title.trim().isEmpty()) title = "ભાગ " + (position + 1);
        holder.title.setText(title);
        if (holder.duration != null) {
            int sec = part != null ? part.getDurationSeconds() : 0;
            holder.duration.setText(formatDuration(sec));
            holder.duration.setVisibility(sec > 0 ? View.VISIBLE : View.GONE);
        }
        boolean isPlaying = part != null && part.getId().equals(currentPlayingPartId);
        holder.setHighlighted(isPlaying);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPartClick(part);
        });
    }

    @Override
    public int getItemCount() {
        return parts.size();
    }

    static class PartHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardView;
        final View leftAccent;
        final TextView partNumber;
        final TextView title;
        final TextView duration;
        final ImageView playingIndicator;

        PartHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            leftAccent = itemView.findViewById(R.id.audio_part_left_accent);
            partNumber = itemView.findViewById(R.id.audio_part_number);
            title = itemView.findViewById(R.id.audio_part_title);
            duration = itemView.findViewById(R.id.audio_part_duration);
            playingIndicator = itemView.findViewById(R.id.audio_part_playing_indicator);
        }

        void setHighlighted(boolean highlighted) {
            if (highlighted) {
                cardView.setCardBackgroundColor(0x1AFF9933); // Light orange
                cardView.setStrokeWidth(0);
                if (leftAccent != null) leftAccent.setVisibility(View.VISIBLE);
                cardView.setCardElevation(4);
                title.setTextColor(0xFFE67E22);
                title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
                if (playingIndicator != null) playingIndicator.setVisibility(View.VISIBLE);
                if (partNumber != null) partNumber.setVisibility(View.GONE);
            } else {
                cardView.setCardBackgroundColor(0xFFFFFFFF);
                if (leftAccent != null) leftAccent.setVisibility(View.GONE);
                cardView.setCardElevation(2);
                title.setTextColor(0xFF212121);
                title.setTypeface(title.getTypeface(), android.graphics.Typeface.NORMAL);
                if (playingIndicator != null) playingIndicator.setVisibility(View.GONE);
                if (partNumber != null) partNumber.setVisibility(View.VISIBLE);
            }
        }
    }

    private static String formatDuration(int seconds) {
        if (seconds <= 0) return "0:00";
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }
}
