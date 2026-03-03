package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter for audio book cards – grid and horizontal layouts.
 * Modern design: 20dp corners, play overlay, progress bar, duration.
 */
public class AudioBookCardAdapter extends RecyclerView.Adapter<AudioBookCardAdapter.CardHolder> {

    private final List<ServerAudioBook> books = new ArrayList<>();
    private boolean fromServer;
    private boolean useCompactLayout;
    private Map<String, Integer> progressMap;
    private Map<String, Integer> durationCache;
    private OnAudioBookClickListener listener;

    public interface OnAudioBookClickListener {
        void onAudioBookClick(ServerAudioBook book);
    }

    public void setBooks(List<ServerAudioBook> list) {
        setBooks(list, false);
    }

    public void setBooks(List<ServerAudioBook> list, boolean fromServer) {
        books.clear();
        if (list != null) books.addAll(list);
        this.fromServer = fromServer;
        notifyDataSetChanged();
    }

    public void setUseCompactLayout(boolean compact) {
        this.useCompactLayout = compact;
    }

    public void setProgressMap(Map<String, Integer> map) {
        this.progressMap = map;
    }

    public void setDurationCache(Map<String, Integer> map) {
        this.durationCache = map;
    }

    public void setOnAudioBookClickListener(OnAudioBookClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = useCompactLayout ? R.layout.item_audio_book_card_horizontal : R.layout.item_audio_book_card;
        View v = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new CardHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CardHolder holder, int position) {
        ServerAudioBook book = books.get(position);
        holder.title.setText(book.getTitle());
        if (holder.author != null) {
            holder.author.setText("સ્વામી સચ્ચિદાનંદ");
            holder.author.setVisibility(View.VISIBLE);
        }

        int totalSec = book.getTotalDurationSeconds();
        if (durationCache != null && book.getId() != null) {
            Integer cached = durationCache.get(book.getId());
            if (cached != null && cached > 0) totalSec = cached;
        }
        if (totalSec <= 0 && book.getParts() != null && !book.getParts().isEmpty()) {
            totalSec = book.getParts().size() * 8 * 60;
        }
        String duration = formatDurationFromSeconds(totalSec);
        if (holder.durationBadge != null) holder.durationBadge.setText(duration);
        if (holder.durationText != null) holder.durationText.setText(duration);

        int percent = 0;
        if (progressMap != null && book.getId() != null) {
            Integer p = progressMap.get(book.getId());
            if (p != null) percent = p;
        }
        if (holder.progressLayout != null) {
            if (percent > 0 && percent < 100) {
                holder.progressLayout.setVisibility(View.VISIBLE);
                if (holder.progressBar != null) holder.progressBar.setProgress(percent);
                if (holder.progressText != null) {
                    holder.progressText.setText(percent + "% ચાલુ રાખો");
                }
            } else {
                holder.progressLayout.setVisibility(View.GONE);
            }
        }

        if (holder.serverBadge != null) holder.serverBadge.setVisibility(View.GONE);
        if (holder.partsBadge != null) holder.partsBadge.setVisibility(View.GONE);
        if (holder.playOverlay != null) holder.playOverlay.setVisibility(View.VISIBLE);

        holder.thumbnail.setImageBitmap(null);
        holder.thumbnail.setImageDrawable(null);
        holder.thumbnail.setBackgroundResource(R.drawable.book_placeholder);
        String thumbUrl = book.getThumbnailUrl();
        android.content.Context ctx = holder.itemView.getContext();

        if ((thumbUrl == null || thumbUrl.isEmpty()) && ctx != null && book.getId() != null && !book.getId().isEmpty()) {
            String base = ctx.getString(R.string.server_books_base_url);
            if (base != null) {
                base = base.trim();
                if (!base.isEmpty() && !base.endsWith("/")) base += "/";
                thumbUrl = base + "thumbnails/" + book.getId() + ".jpg";
            }
        }

        if (thumbUrl != null && !thumbUrl.isEmpty() && ctx != null) {
            Glide.with(ctx)
                    .load(thumbUrl)
                    .apply(new RequestOptions()
                            .transform(new RoundedCorners(useCompactLayout ? 12 : 16))
                            .centerCrop())
                    .placeholder(R.drawable.book_placeholder)
                    .error(R.drawable.book_placeholder)
                    .into(holder.thumbnail);
        } else {
            holder.thumbnail.setImageDrawable(null);
            holder.thumbnail.setBackgroundResource(R.drawable.book_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAudioBookClick(book);
        });
    }

    private static String formatDurationFromSeconds(int totalSeconds) {
        if (totalSeconds <= 0) return "0:00";
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        if (m > 0 || s > 0) return String.format("%d:%02d", m, s);
        return "0:00";
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    static class CardHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final TextView title;
        final TextView author;
        final TextView durationBadge;
        final TextView durationText;
        final LinearLayout progressLayout;
        final ProgressBar progressBar;
        final TextView progressText;
        final TextView partsBadge;
        final TextView serverBadge;
        final ImageView playOverlay;

        CardHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.audio_book_card_thumbnail);
            title = itemView.findViewById(R.id.audio_book_card_title);
            author = itemView.findViewById(R.id.audio_book_card_author);
            durationBadge = itemView.findViewById(R.id.audio_book_card_duration_badge);
            durationText = itemView.findViewById(R.id.audio_book_card_duration);
            progressLayout = itemView.findViewById(R.id.audio_book_card_progress_layout);
            progressBar = itemView.findViewById(R.id.audio_book_card_progress_bar);
            progressText = itemView.findViewById(R.id.audio_book_card_progress_text);
            partsBadge = itemView.findViewById(R.id.audio_book_card_parts);
            serverBadge = itemView.findViewById(R.id.audio_book_card_server_badge);
            playOverlay = itemView.findViewById(R.id.audio_book_card_play_overlay);
        }
    }
}
