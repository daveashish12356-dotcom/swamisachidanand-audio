package com.swamisachidanand;

import android.graphics.Bitmap;
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

import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> books;
    private OnBookClickListener listener;
    private PdfThumbnailLoader thumbnailLoader;

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
            String pdfUrl = book.getPdfUrl();
            if (holder.bookOnlineBadge != null) {
                holder.bookOnlineBadge.setVisibility(pdfUrl != null && !pdfUrl.trim().isEmpty() ? View.VISIBLE : View.GONE);
            }
            if (holder.bookThumbnail != null) {
                holder.bookThumbnail.setImageBitmap(null);
                holder.bookThumbnail.setImageDrawable(null);
                holder.bookThumbnail.setBackgroundResource(R.drawable.book_placeholder);
                android.content.Context ctx = holder.itemView != null ? holder.itemView.getContext() : null;
                String thumbnailUrl = book.getThumbnailUrl();
                if (ctx != null && thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                    Glide.with(ctx).load(thumbnailUrl)
                            .apply(new RequestOptions().transform(new RoundedCorners(8)))
                            .placeholder(R.drawable.book_placeholder)
                            .error(R.drawable.book_placeholder)
                            .into(holder.bookThumbnail);
                    holder.bookThumbnail.setBackground(null);
                } else if (thumbnailLoader != null && ctx != null && book.getFileName() != null) {
                    thumbnailLoader.loadThumbnail(ctx, book.getFileName(), thumbnail -> {
                        if (holder.bookThumbnail == null) return;
                        if (thumbnail != null && !thumbnail.isRecycled()) {
                            holder.bookThumbnail.setImageBitmap(thumbnail);
                            holder.bookThumbnail.setBackground(null);
                        } else {
                            holder.bookThumbnail.setBackgroundResource(R.drawable.book_placeholder);
                        }
                    });
                }
            }
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onBookClick(book);
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

        BookViewHolder(View itemView) {
            super(itemView);
            bookThumbnail = itemView.findViewById(R.id.book_thumbnail);
            bookName = itemView.findViewById(R.id.book_name);
            bookYear = itemView.findViewById(R.id.book_year);
            bookOnlineBadge = itemView.findViewById(R.id.book_online_badge);
        }
    }
}

