package com.swamisachidanand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Single row history: books, audio books, and videos in one horizontal list. */
public class HomeHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<HomeHistoryItem> items = new ArrayList<>();
    private Map<String, Integer> audioProgressMap;
    private PdfThumbnailLoader thumbnailLoader;
    private Listener listener;

    public interface Listener {
        void onBookClick(Book book);
        void onAudioClick(ServerAudioBook book);
        void onVideoClick(HomeVideoLoader.HomeVideoItem video);
    }

    public void setItems(List<HomeHistoryItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void setAudioProgressMap(Map<String, Integer> map) {
        this.audioProgressMap = map;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= items.size()) return HomeHistoryItem.TYPE_BOOK;
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (thumbnailLoader == null) thumbnailLoader = PdfThumbnailLoader.getInstance();
        int w = parent.getResources().getDimensionPixelSize(R.dimen.history_card_width);
        int m = parent.getResources().getDimensionPixelSize(R.dimen.history_card_margin);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(w, RecyclerView.LayoutParams.WRAP_CONTENT);
        lp.setMargins(m, 0, m, 0);
        View v;
        if (viewType == HomeHistoryItem.TYPE_BOOK) {
            v = inf.inflate(R.layout.item_history_book, parent, false);
            v.setLayoutParams(lp);
            return new BookHolder(v);
        }
        if (viewType == HomeHistoryItem.TYPE_AUDIO) {
            v = inf.inflate(R.layout.item_history_audio, parent, false);
            v.setLayoutParams(lp);
            return new AudioHolder(v);
        }
        v = inf.inflate(R.layout.item_history_video, parent, false);
        v.setLayoutParams(lp);
        return new VideoHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) return;
        HomeHistoryItem item = items.get(position);
        if (holder instanceof BookHolder) {
            bindBook((BookHolder) holder, (Book) item.data);
        } else if (holder instanceof AudioHolder) {
            bindAudio((AudioHolder) holder, (ServerAudioBook) item.data);
        } else if (holder instanceof VideoHolder) {
            bindVideo((VideoHolder) holder, (HomeVideoLoader.HomeVideoItem) item.data);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindBook(BookHolder h, Book book) {
        if (book == null) return;
        h.name.setText(book.getName() != null ? book.getName() : "");
        String year = book.getPublishYear();
        if (year != null && !year.isEmpty()) {
            h.year.setText(year);
            h.year.setVisibility(View.VISIBLE);
        } else {
            h.year.setVisibility(View.GONE);
        }
        String thumbUrl = book.getThumbnailUrl();
        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            Glide.with(h.itemView.getContext()).load(thumbUrl)
                    .apply(new RequestOptions().transform(new RoundedCorners(8)).centerCrop())
                    .placeholder(R.drawable.book_placeholder).error(R.drawable.book_placeholder)
                    .into(h.thumb);
        } else if (book.getFileName() != null) {
            thumbnailLoader.loadThumbnail(h.itemView.getContext(), book.getFileName(), bm -> {
                if (h.thumb != null && bm != null && !bm.isRecycled())
                    h.thumb.setImageBitmap(bm);
                else if (h.thumb != null) h.thumb.setImageResource(R.drawable.book_placeholder);
            });
        } else {
            h.thumb.setImageResource(R.drawable.book_placeholder);
        }
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onBookClick(book); });
    }

    private void bindAudio(AudioHolder h, ServerAudioBook book) {
        if (book == null) return;
        h.title.setText(book.getTitle() != null ? book.getTitle() : "");
        int progress = 0;
        if (audioProgressMap != null && book.getId() != null) {
            Integer p = audioProgressMap.get(book.getId());
            if (p != null) progress = p;
        }
        if (progress > 0 && progress < 100 && h.progressLayout != null) {
            h.progressLayout.setVisibility(View.VISIBLE);
            if (h.progressBar != null) h.progressBar.setProgress(progress);
            if (h.progressText != null) h.progressText.setText(progress + "% ચાલુ રાખો");
        } else if (h.progressLayout != null) {
            h.progressLayout.setVisibility(View.GONE);
        }
        android.content.Context ctx = h.itemView.getContext();
        String thumbUrl = book.getThumbnailUrl();
        if ((thumbUrl == null || thumbUrl.isEmpty()) && ctx != null && book.getId() != null && !book.getId().isEmpty()) {
            String base = ctx.getString(R.string.server_books_base_url);
            if (base != null) {
                base = base.trim();
                if (!base.isEmpty() && !base.endsWith("/")) base += "/";
                thumbUrl = base + "public/thumbnails/" + book.getId() + ".jpg";
            }
        }
        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            Glide.with(ctx).load(thumbUrl)
                    .apply(new RequestOptions().centerCrop())
                    .placeholder(R.drawable.book_placeholder).error(R.drawable.book_placeholder)
                    .into(h.thumb);
        } else {
            h.thumb.setImageResource(R.drawable.book_placeholder);
        }
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onAudioClick(book); });
    }

    private void bindVideo(VideoHolder h, HomeVideoLoader.HomeVideoItem video) {
        if (video == null) return;
        h.title.setText(video.title != null ? video.title : "વિડિઓ");
        h.meta.setText("");
        h.duration.setVisibility(View.GONE);
        String thumbUrl = video.thumbnailUrl;
        if (thumbUrl == null && video.videoId != null)
            thumbUrl = "https://img.youtube.com/vi/" + video.videoId + "/default.jpg";
        if (thumbUrl != null) {
            Glide.with(h.itemView.getContext()).load(thumbUrl)
                    .centerCrop().placeholder(R.drawable.book_placeholder).error(R.drawable.book_placeholder)
                    .into(h.thumb);
        } else {
            h.thumb.setImageResource(R.drawable.book_placeholder);
        }
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onVideoClick(video); });
    }

    static class BookHolder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView name, year;
        BookHolder(View v) {
            super(v);
            thumb = v.findViewById(R.id.book_thumbnail);
            name = v.findViewById(R.id.book_name);
            year = v.findViewById(R.id.book_year);
        }
    }

    static class AudioHolder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView title;
        final View progressLayout;
        final android.widget.ProgressBar progressBar;
        final TextView progressText;
        AudioHolder(View v) {
            super(v);
            thumb = v.findViewById(R.id.audio_book_card_thumbnail);
            title = v.findViewById(R.id.audio_book_card_title);
            progressLayout = v.findViewById(R.id.audio_book_card_progress_layout);
            progressBar = v.findViewById(R.id.audio_book_card_progress_bar);
            progressText = v.findViewById(R.id.audio_book_card_progress_text);
        }
    }

    static class VideoHolder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final TextView title, meta, duration;
        VideoHolder(View v) {
            super(v);
            thumb = v.findViewById(R.id.video_thumb);
            title = v.findViewById(R.id.video_title);
            meta = v.findViewById(R.id.video_meta);
            duration = v.findViewById(R.id.video_duration);
        }
    }
}
