package com.swamisachidanand;

import android.content.Intent;
import android.net.Uri;
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

/**
 * એક લાઇનમાં પુસ્તક + ઓડિયો + વિડિઓ – search result સૌ સાથે (akhiline).
 */
public class UnifiedSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int TYPE_BOOK = 0;
    static final int TYPE_AUDIO = 1;
    static final int TYPE_VIDEO = 2;

    private final List<UnifiedSearchItem> items = new ArrayList<>();
    private BookAdapter.OnBookClickListener bookListener;
    private AudioBookCardAdapter.OnAudioBookClickListener audioListener;

    public void setItems(List<Book> books, List<ServerAudioBook> audio, List<HomeVideoLoader.HomeVideoItem> videos) {
        items.clear();
        if (books != null) {
            for (Book b : books) items.add(new UnifiedSearchItem(TYPE_BOOK, b));
        }
        if (audio != null) {
            for (ServerAudioBook a : audio) items.add(new UnifiedSearchItem(TYPE_AUDIO, a));
        }
        if (videos != null) {
            for (HomeVideoLoader.HomeVideoItem v : videos) items.add(new UnifiedSearchItem(TYPE_VIDEO, v));
        }
        notifyDataSetChanged();
    }

    public void setBookClickListener(BookAdapter.OnBookClickListener l) { bookListener = l; }
    public void setAudioClickListener(AudioBookCardAdapter.OnAudioBookClickListener l) { audioListener = l; }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_BOOK) {
            View v = inf.inflate(R.layout.item_book_home, parent, false);
            return new BookVH(v);
        }
        if (viewType == TYPE_AUDIO) {
            View v = inf.inflate(R.layout.item_audio_book_card_horizontal, parent, false);
            return new AudioVH(v);
        }
        View v = inf.inflate(R.layout.item_video_horizontal, parent, false);
        return new VideoVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        UnifiedSearchItem item = items.get(position);
        if (holder instanceof BookVH && item.data instanceof Book) {
            bindBook((BookVH) holder, (Book) item.data);
        } else if (holder instanceof AudioVH && item.data instanceof ServerAudioBook) {
            bindAudio((AudioVH) holder, (ServerAudioBook) item.data);
        } else if (holder instanceof VideoVH && item.data instanceof HomeVideoLoader.HomeVideoItem) {
            bindVideo((VideoVH) holder, (HomeVideoLoader.HomeVideoItem) item.data);
        }
    }

    private void bindBook(BookVH h, Book book) {
        h.bookName.setText(book.getName() != null ? book.getName() : "");
        if (h.bookYear != null) h.bookYear.setVisibility(View.GONE);
        if (h.bookOnlineBadge != null) h.bookOnlineBadge.setVisibility(View.GONE);
        String thumbUrl = book.getThumbnailUrl();
        if (thumbUrl != null && !thumbUrl.isEmpty() && h.itemView.getContext() != null) {
            Glide.with(h.itemView.getContext())
                    .load(thumbUrl)
                    .apply(new RequestOptions().centerCrop())
                    .placeholder(R.drawable.book_placeholder)
                    .error(R.drawable.book_placeholder)
                    .into(h.bookThumbnail);
        } else {
            h.bookThumbnail.setImageResource(R.drawable.book_placeholder);
        }
        h.itemView.setOnClickListener(v -> {
            if (bookListener != null) bookListener.onBookClick(book);
        });
    }

    private void bindAudio(AudioVH h, ServerAudioBook book) {
        h.title.setText(book.getTitle());
        if (h.author != null) {
            h.author.setText("સ્વામી સચ્ચિદાનંદ");
            h.author.setVisibility(View.VISIBLE);
        }
        int totalSec = book.getTotalDurationSeconds();
        if (totalSec <= 0 && book.getParts() != null && !book.getParts().isEmpty()) {
            totalSec = book.getParts().size() * 8 * 60;
        }
        String duration = formatDuration(totalSec);
        if (h.durationBadge != null) h.durationBadge.setText(duration);
        if (h.durationText != null) h.durationText.setText(duration);
        if (h.progressLayout != null) h.progressLayout.setVisibility(View.GONE);
        if (h.playOverlay != null) h.playOverlay.setVisibility(View.VISIBLE);
        h.thumbnail.setImageDrawable(null);
        h.thumbnail.setBackgroundResource(R.drawable.book_placeholder);
        String thumbUrl = book.getThumbnailUrl();
        if (thumbUrl == null || thumbUrl.isEmpty()) {
            if (h.itemView.getContext() != null && book.getId() != null) {
                String base = h.itemView.getContext().getString(R.string.server_books_base_url);
                if (base != null && !base.trim().isEmpty()) {
                    base = base.trim();
                    if (!base.endsWith("/")) base += "/";
                    thumbUrl = base + "public/thumbnails/" + book.getId() + ".jpg";
                }
            }
        }
        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(thumbUrl)
                    .apply(new RequestOptions().transform(new RoundedCorners(12)).centerCrop())
                    .placeholder(R.drawable.book_placeholder)
                    .error(R.drawable.book_placeholder)
                    .into(h.thumbnail);
        }
        h.itemView.setOnClickListener(v -> {
            if (audioListener != null) audioListener.onAudioBookClick(book);
        });
    }

    private void bindVideo(VideoVH h, HomeVideoLoader.HomeVideoItem item) {
        h.title.setText(item.title != null ? item.title : "");
        String meta = HomeVideoLoader.toRelativeTime(item.publishedAt) + (item.viewCount >= 0 ? " • " + HomeVideoLoader.formatViewCount(item.viewCount) : "");
        h.meta.setText(meta);
        if (item.durationSeconds >= 0) {
            h.duration.setText(HomeVideoLoader.formatDuration(item.durationSeconds));
            h.duration.setVisibility(View.VISIBLE);
        } else {
            h.duration.setVisibility(View.GONE);
        }
        if (item.thumbnailUrl != null && !item.thumbnailUrl.isEmpty()) {
            Glide.with(h.thumb.getContext()).load(item.thumbnailUrl)
                    .placeholder(R.drawable.book_placeholder).error(R.drawable.book_placeholder)
                    .centerCrop().into(h.thumb);
        } else {
            h.thumb.setImageResource(R.drawable.book_placeholder);
        }
        h.itemView.setOnClickListener(v -> {
            if (item.videoId != null) {
                RecentVideoHelper.saveRecentVideoId(v.getContext(), item.videoId);
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + item.videoId)));
            }
        });
    }

    private static String formatDuration(int totalSeconds) {
        if (totalSeconds <= 0) return "0:00";
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UnifiedSearchItem {
        final int type;
        final Object data;
        UnifiedSearchItem(int type, Object data) {
            this.type = type;
            this.data = data;
        }
    }

    static class BookVH extends RecyclerView.ViewHolder {
        final ImageView bookThumbnail;
        final TextView bookName;
        final TextView bookYear;
        final TextView bookOnlineBadge;
        BookVH(View v) {
            super(v);
            bookThumbnail = v.findViewById(R.id.book_thumbnail);
            bookName = v.findViewById(R.id.book_name);
            bookYear = v.findViewById(R.id.book_year);
            bookOnlineBadge = v.findViewById(R.id.book_online_badge);
        }
    }

    static class AudioVH extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final TextView title;
        final TextView author;
        final TextView durationBadge;
        final TextView durationText;
        final LinearLayout progressLayout;
        final ImageView playOverlay;
        AudioVH(View v) {
            super(v);
            thumbnail = v.findViewById(R.id.audio_book_card_thumbnail);
            title = v.findViewById(R.id.audio_book_card_title);
            author = v.findViewById(R.id.audio_book_card_author);
            durationBadge = v.findViewById(R.id.audio_book_card_duration_badge);
            durationText = v.findViewById(R.id.audio_book_card_duration);
            progressLayout = v.findViewById(R.id.audio_book_card_progress_layout);
            playOverlay = v.findViewById(R.id.audio_book_card_play_overlay);
        }
    }

    static class VideoVH extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView title;
        final TextView meta;
        final TextView duration;
        VideoVH(View v) {
            super(v);
            thumb = v.findViewById(R.id.video_thumb);
            title = v.findViewById(R.id.video_title);
            meta = v.findViewById(R.id.video_meta);
            duration = v.findViewById(R.id.video_duration);
        }
    }
}
