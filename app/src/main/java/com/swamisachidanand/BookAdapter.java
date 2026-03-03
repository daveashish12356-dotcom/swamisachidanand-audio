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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;
import java.util.Map;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> books;
    private OnBookClickListener listener;
    private PdfThumbnailLoader thumbnailLoader;
    private Map<String, Integer> readingProgressMap;

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    private boolean useCompactLayout = false;

    public BookAdapter(List<Book> books, OnBookClickListener listener) {
        this.books = (books != null) ? books : new java.util.ArrayList<>();
        this.listener = listener;
        this.thumbnailLoader = PdfThumbnailLoader.getInstance();
    }
    
    public void setUseCompactLayout(boolean useCompact) {
        this.useCompactLayout = useCompact;
    }

    public void setReadingProgressMap(Map<String, Integer> map) {
        this.readingProgressMap = map;
    }

    public void updateBooks(List<Book> newBooks) {
        this.books = (newBooks != null) ? newBooks : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = useCompactLayout ? R.layout.item_book_home : R.layout.item_book;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        try {
            if (books == null || position < 0 || position >= books.size()) return;
            Book book = books.get(position);
            if (book == null) return;
            if (holder.bookName != null) holder.bookName.setText(book.getName() != null ? book.getName() : "");
            String year = book.getPublishYear();
            if (holder.bookYear != null) {
                if (year != null && !year.isEmpty()) {
                    holder.bookYear.setText(year);
                    holder.bookYear.setVisibility(View.VISIBLE);
                } else {
                    holder.bookYear.setVisibility(View.GONE);
                }
            }
            // Reading progress (grid layout only)
            if (!useCompactLayout && holder.readingProgressLayout != null) {
                int percent = 0;
                if (readingProgressMap != null && book.getName() != null) {
                    Integer p = readingProgressMap.get(book.getName());
                    if (p == null && book.getFileName() != null) {
                        p = readingProgressMap.get(book.getFileName().replace(".pdf", "").replace(".PDF", ""));
                    }
                    if (p != null) percent = p;
                }
                if (percent > 0 && percent < 100) {
                    holder.readingProgressLayout.setVisibility(View.VISIBLE);
                    if (holder.readingProgressBar != null) holder.readingProgressBar.setProgress(percent);
                    if (holder.readingProgressText != null) {
                        holder.readingProgressText.setText(percent + "% વાંચવાનું ચાલુ રાખો");
                    }
                } else {
                    holder.readingProgressLayout.setVisibility(View.GONE);
                }
            }
            if (holder.bookOnlineBadge != null) {
                holder.bookOnlineBadge.setVisibility(View.GONE);
            }
            if (holder.bookThumbnail != null) {
                holder.bookThumbnail.setImageBitmap(null);
                holder.bookThumbnail.setImageDrawable(null);
                holder.bookThumbnail.setBackground(null);
                android.content.Context ctx = holder.itemView != null ? holder.itemView.getContext() : null;
                String thumbnailUrl = book.getThumbnailUrl();
                if (ctx != null && thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                    Glide.with(ctx).load(thumbnailUrl)
                            .apply(new RequestOptions()
                                    .transform(new RoundedCorners(8))
                                    .override(300, 400)
                                    .centerCrop()
                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                            .placeholder(R.drawable.book_placeholder)
                            .error(R.drawable.book_placeholder)
                            .thumbnail(0.15f)
                            .into(holder.bookThumbnail);
                } else if (thumbnailLoader != null && ctx != null && book.getFileName() != null && !book.getFileName().isEmpty()) {
                    thumbnailLoader.loadThumbnail(ctx, book.getFileName(), thumbnail -> {
                        if (holder.bookThumbnail == null) return;
                        if (thumbnail != null && !thumbnail.isRecycled()) {
                            holder.bookThumbnail.setImageBitmap(thumbnail);
                            holder.bookThumbnail.setBackground(null);
                        } else {
                            holder.bookThumbnail.setImageResource(R.drawable.book_placeholder);
                        }
                    });
                } else {
                    holder.bookThumbnail.setImageResource(R.drawable.book_placeholder);
                }
            }
            // 3D press animation – thoda sa scale effect
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBookClick(book);
            });
            holder.itemView.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(1.03f).scaleY(1.03f).setDuration(120).start();
                        break;
                    case android.view.MotionEvent.ACTION_CANCEL:
                    case android.view.MotionEvent.ACTION_UP:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                        break;
                }
                return false;
            });
        } catch (Throwable t) {
            android.util.Log.e("BookAdapter", "onBindViewHolder", t);
        }
    }

    @Override
    public int getItemCount() {
        return (books != null) ? books.size() : 0;
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        ImageView bookThumbnail;
        TextView bookName;
        TextView bookYear;
        TextView bookOnlineBadge;
        LinearLayout readingProgressLayout;
        ProgressBar readingProgressBar;
        TextView readingProgressText;

        BookViewHolder(View itemView) {
            super(itemView);
            bookThumbnail = itemView.findViewById(R.id.book_thumbnail);
            bookName = itemView.findViewById(R.id.book_name);
            bookYear = itemView.findViewById(R.id.book_year);
            bookOnlineBadge = itemView.findViewById(R.id.book_online_badge);
            readingProgressLayout = itemView.findViewById(R.id.reading_progress_layout);
            readingProgressBar = itemView.findViewById(R.id.reading_progress_bar);
            readingProgressText = itemView.findViewById(R.id.reading_progress_text);
        }
    }
}

